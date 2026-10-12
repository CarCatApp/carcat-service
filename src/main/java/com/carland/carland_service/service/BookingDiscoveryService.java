package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBranchListResponse;
import com.carland.carland_service.dto.booking.BookingDiscoveryBranchView;
import com.carland.carland_service.dto.booking.BookingDiscoveryPartnerView;
import com.carland.carland_service.dto.booking.BookingDiscoveryResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.CalendarStatus;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.IndividualServiceSourceRepository;
import com.carland.carland_service.repository.RangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private final IndividualServiceSourceRepository individualServiceSourceRepository;
    private final BranchIndividualServiceRepository branchIndividualServiceRepository;
    private final RangeRepository rangeRepository;

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
                    .rating(partner.getRating())
                    .ratingCount(BookingRatingService.storedCount(partner.getRatingCount()))
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

    /**
     * tr: findService yoksa aktif şubelerin hepsi. Varsa services.id bağlı fərdi xidməti
     *     açık, fiyatı yazılmış ve bugünden sonra yazılabilir saati olan şubeler.
     * en: Without findService, every active branch. With it, branches that have the linked
     *     individual service on, a price set, and a bookable hour from now on.
     */
    @Transactional(readOnly = true)
    public BookingBranchListResponse listActive(Long findService) {
        List<Branch> loaded = branchRepository.findActiveWithActivePartner();
        if (findService != null) {
            Set<Long> ready = readyBranchIds(findService);
            loaded = loaded.stream().filter(branch -> ready.contains(branch.getId())).toList();
        }
        List<BookingDiscoveryBranchView> branches = new ArrayList<>();
        for (Branch branch : loaded) {
            branches.add(toBranch(branch));
        }
        return BookingBranchListResponse.builder().branches(branches).build();
    }

    /** services.id is the source key, so the list is empty or one individual-service id. */
    private Set<Long> readyBranchIds(Long catalogServiceId) {
        List<Long> linked = individualServiceSourceRepository.findIndividualServiceIdsByServiceId(catalogServiceId);
        if (linked == null || linked.isEmpty()) {
            return Set.of();
        }
        OffsetDateTime now = OffsetDateTime.now(StaffSlotWindows.ZONE);
        Set<Long> ready = new HashSet<>();
        for (Long individualServiceId : linked) {
            if (individualServiceId == null) {
                continue;
            }
            Set<Long> priced = new HashSet<>(
                    branchIndividualServiceRepository.findActivePricedBranchIds(individualServiceId));
            if (priced.isEmpty()) {
                continue;
            }
            priced.retainAll(rangeRepository.findBranchIdsWithBookableIndividualSlot(
                    individualServiceId,
                    StaffSlotTargets.INDIVIDUAL,
                    RangeStatus.AVAILABLE.name(),
                    now,
                    CalendarStatus.HIDDEN.name(),
                    BookingStatus.occupyingCapacity()));
            ready.addAll(priced);
        }
        return ready;
    }

    private static BookingDiscoveryBranchView toBranch(Branch branch) {
        Partner partner = branch.getPartner();
        return BookingDiscoveryBranchView.builder()
                .partnerId(partner.getId())
                .partnerName(partner.getName())
                .branchId(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .lat(branch.getLat())
                .lng(branch.getLng())
                .active(Boolean.TRUE.equals(branch.getActive()))
                .contactPhone(branch.getContactPhone())
                .workingHours(branch.getWorkingHours())
                .workingHoursWeekday(BranchWorkingHours.weekdayOf(branch))
                .workingHoursSaturday(BranchWorkingHours.saturdayOf(branch))
                .workingHoursSunday(BranchWorkingHours.sundayOf(branch))
                .workingHoursWeekend(branch.getWorkingHoursWeekend())
                .verified(Boolean.TRUE.equals(branch.getVerified()))
                .photo(branch.getPhoto())
                .rating(branch.getRating())
                .ratingCount(BookingRatingService.storedCount(branch.getRatingCount()))
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
