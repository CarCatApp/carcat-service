package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingDiscoveryBranchView;
import com.carland.carland_service.dto.booking.BookingDiscoveryPartnerView;
import com.carland.carland_service.dto.booking.BookingDiscoveryResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * tr: Müşteri uygulaması şube keşfi. Sadece aktif partner + aktif şube.
 * en: Owner-app branch discovery. Active partner + active branch only.
 */
@Service
@RequiredArgsConstructor
public class BookingDiscoveryService {

    static final int DEFAULT_PAGE = 1;
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;
    private static final double EARTH_M = 6_371_000d;

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public BookingDiscoveryResponse discover(Long partnerId, String q, Double lat, Double lng, Integer radiusMeters,
                                             Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int safeSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        boolean geo = lat != null && lng != null && radiusMeters != null && radiusMeters > 0;
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        List<Branch> loaded = branchRepository.findActiveWithActivePartner();
        Map<Long, List<Branch>> grouped = new LinkedHashMap<>();
        for (Branch branch : loaded) {
            Partner partner = branch.getPartner();
            if (partnerId != null && !partnerId.equals(partner.getId())) {
                continue;
            }
            if (geo) {
                if (branch.getLat() == null || branch.getLng() == null) {
                    continue;
                }
                if (meters(lat, lng, branch.getLat(), branch.getLng()) > radiusMeters) {
                    continue;
                }
            }
            if (!needle.isEmpty() && !matches(partner.getName(), needle) && !matches(branch.getName(), needle)
                    && !matches(branch.getAddress(), needle)) {
                continue;
            }
            grouped.computeIfAbsent(partner.getId(), ignored -> new ArrayList<>()).add(branch);
        }

        List<BookingDiscoveryPartnerView> all = new ArrayList<>();
        for (List<Branch> branches : grouped.values()) {
            if (geo) {
                branches.sort(Comparator.comparingDouble(b -> meters(lat, lng, b.getLat(), b.getLng())));
            }
            Partner partner = branches.get(0).getPartner();
            List<BookingDiscoveryBranchView> views = new ArrayList<>();
            for (Branch branch : branches) {
                views.add(toBranch(branch));
            }
            all.add(BookingDiscoveryPartnerView.builder()
                    .partnerId(partner.getId())
                    .name(partner.getName())
                    .branches(views)
                    .build());
        }

        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return BookingDiscoveryResponse.builder()
                .page(safePage)
                .pageSize(safeSize)
                .partners(new ArrayList<>(all.subList(from, to)))
                .build();
    }

    private static BookingDiscoveryBranchView toBranch(Branch branch) {
        Partner partner = branch.getPartner();
        return BookingDiscoveryBranchView.builder()
                .partnerId(partner.getId())
                .branchId(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .lat(branch.getLat())
                .lng(branch.getLng())
                .active(Boolean.TRUE.equals(branch.getActive()))
                .contactPhone(branch.getContactPhone())
                .workingHours(branch.getWorkingHours())
                .photo(branch.getPhoto())
                .ratingCount(branch.getRatingCount())
                .build();
    }

    private static boolean matches(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    static double meters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_M * Math.asin(Math.min(1d, Math.sqrt(a)));
    }
}
