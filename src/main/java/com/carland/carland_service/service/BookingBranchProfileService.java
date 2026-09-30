package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBranchProfileProductView;
import com.carland.carland_service.dto.booking.BookingBranchProfileResponse;
import com.carland.carland_service.dto.booking.BookingBranchProfileServiceView;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchServiceCategory;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.ServiceCategory;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServiceCategoryRepository;
import com.carland.carland_service.repository.PartnerPhotoRepository;
import com.carland.carland_service.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tr: Müşteri şube profili. Paket sayısı care paketlerden. Hizmet sayısı ve marka grupları partner-ui yazılınca dolar.
 * en: Owner branch profile. Package count comes from care packages. Service count and brand groups stay empty until partner-ui writes them.
 */
@Service
@RequiredArgsConstructor
public class BookingBranchProfileService {

    static final String ROUTINE = "routine";
    static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    private final BranchRepository branchRepository;
    private final PartnerPhotoRepository partnerPhotoRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final BranchServiceCategoryRepository branchCategoryRepository;
    private final BranchCarePackageRepository carePackageRepository;
    private final ServiceCategoryJson json;

    @Transactional(readOnly = true)
    public BookingBranchProfileResponse profile(Long branchId, Double lat, Double lng, String acceptLanguage) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("branch not found"));
        Partner partner = branch.getPartner();
        if (!Boolean.TRUE.equals(branch.getActive())
                || partner == null
                || !Boolean.TRUE.equals(partner.getActive())) {
            throw new ResourceNotFoundException("branch not found");
        }
        int packages = (int) carePackageRepository.countByBranch_IdAndActiveTrue(branch.getId());
        String lang = BookingMineService.langOf(acceptLanguage);
        return BookingBranchProfileResponse.builder()
                .branchId(branch.getId())
                .name(branch.getName())
                .partnerId(partner.getId())
                .partnerName(partner.getName())
                .verified(Boolean.TRUE.equals(branch.getVerified()))
                .logoUrl(logoUrl(partner.getId()))
                .address(branch.getAddress())
                .lat(branch.getLat())
                .lng(branch.getLng())
                .distanceKm(distanceKm(lat, lng, branch.getLat(), branch.getLng()))
                .workingHoursWeekday(branch.getWorkingHoursWeekday())
                .workingHoursWeekend(branch.getWorkingHoursWeekend())
                .open(openNow(branch.getWorkingHoursWeekday(), branch.getWorkingHoursWeekend(), OffsetDateTime.now()))
                .services(servicesOf(branch.getId(), packages, lang))
                .products(List.of())
                .build();
    }

    private List<BookingBranchProfileServiceView> servicesOf(Long branchId, int packageCount, String lang) {
        Map<Long, Boolean> flags = new HashMap<>();
        for (BranchServiceCategory row : branchCategoryRepository.findByBranch_Id(branchId)) {
            if (row.getCategory() != null && row.getCategory().getId() != null) {
                flags.put(row.getCategory().getId(), row.getActive());
            }
        }
        List<BookingBranchProfileServiceView> items = new ArrayList<>();
        for (ServiceCategory category : categoryRepository.findByActiveTrueOrderBySortOrderAscIdAsc()) {
            if (!shown(category, flags)) {
                continue;
            }
            boolean routine = ROUTINE.equals(category.getCode());
            items.add(BookingBranchProfileServiceView.builder()
                    .id(category.getId())
                    .code(category.getCode())
                    .name(BookingMineService.catalogText(json.read(category.getTitleJson()), lang))
                    .packageCount(routine ? packageCount : null)
                    .serviceCount(null)
                    .build());
        }
        return items;
    }

    private static boolean shown(ServiceCategory category, Map<Long, Boolean> flags) {
        if (!Boolean.TRUE.equals(category.getToggleable())) {
            return true;
        }
        return Boolean.TRUE.equals(flags.get(category.getId()));
    }

    private String logoUrl(Long partnerId) {
        if (partnerId == null || !partnerPhotoRepository.existsByPartnerId(partnerId)) {
            return null;
        }
        return BookingMineService.LOGO_PATH + partnerId;
    }

    static Double distanceKm(Double fromLat, Double fromLng, Double branchLat, Double branchLng) {
        if (fromLat == null || fromLng == null || branchLat == null || branchLng == null) {
            return null;
        }
        double meters = BookingDiscoveryService.meters(fromLat, fromLng, branchLat, branchLng);
        return Math.round(meters / 100d) / 10d;
    }

    static Boolean openNow(String weekday, String weekend, OffsetDateTime now) {
        if (now == null) {
            return null;
        }
        ZonedDateTime zoned = now.atZoneSameInstant(BAKU);
        DayOfWeek day = zoned.getDayOfWeek();
        boolean weekendDay = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        int[] range = parseRange(weekendDay ? weekend : weekday);
        if (range == null) {
            return null;
        }
        int minutes = zoned.getHour() * 60 + zoned.getMinute();
        return minutes >= range[0] && minutes < range[1];
    }

    private static int[] parseRange(String raw) {
        if (raw == null || !raw.contains("-")) {
            return null;
        }
        String[] parts = raw.split("-", 2);
        Integer start = clock(parts[0]);
        Integer end = clock(parts[1]);
        if (start == null || end == null || end <= start) {
            return null;
        }
        return new int[]{start, end};
    }

    private static Integer clock(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.length() < 5) {
            return null;
        }
        try {
            int hour = Integer.parseInt(value.substring(0, 2));
            int minute = Integer.parseInt(value.substring(3, 5));
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || value.charAt(2) != ':') {
                return null;
            }
            return hour * 60 + minute;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
