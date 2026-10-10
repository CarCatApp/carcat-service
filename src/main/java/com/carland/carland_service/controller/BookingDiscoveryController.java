package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.BookingAvailabilityResponse;
import com.carland.carland_service.dto.booking.BookingCalendarRequest;
import com.carland.carland_service.dto.booking.BookingCalendarResponse;
import com.carland.carland_service.dto.booking.BookingDayRangesRequest;
import com.carland.carland_service.dto.booking.BookingDayRangesResponse;
import com.carland.carland_service.dto.booking.BookingCarePackagesResponse;
import com.carland.carland_service.dto.booking.BookingBranchListResponse;
import com.carland.carland_service.dto.booking.BookingBranchProfileResponse;
import com.carland.carland_service.dto.booking.BookingBrandModelsResponse;
import com.carland.carland_service.dto.booking.BookingIndividualServiceFiltersResponse;
import com.carland.carland_service.dto.booking.BookingIndividualServicesResponse;
import com.carland.carland_service.dto.booking.BookingPackagePriceInfoResponse;
import com.carland.carland_service.dto.booking.BookingCatalogResponse;
import com.carland.carland_service.dto.booking.BookingDiscoveryResponse;
import com.carland.carland_service.service.BookingAvailabilityService;
import com.carland.carland_service.service.BookingCalendarService;
import com.carland.carland_service.service.BookingCarePackageCatalogService;
import com.carland.carland_service.service.BookingBranchBrandsService;
import com.carland.carland_service.service.BookingIndividualCatalogService;
import com.carland.carland_service.service.BookingPackagePriceInfoService;
import com.carland.carland_service.service.BookingBranchProfileService;
import com.carland.carland_service.service.BookingCatalogService;
import com.carland.carland_service.service.BookingDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.carland.carland_service.exceptions.MissingFieldException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Müşteri keşif + katalog + müsaitlik. Flag: booking.
 * en: Owner discovery + catalog + availability. Flag: booking.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class BookingDiscoveryController {

    private final BookingDiscoveryService bookingDiscoveryService;
    private final BookingCatalogService bookingCatalogService;
    private final BookingAvailabilityService bookingAvailabilityService;
    private final BookingCalendarService bookingCalendarService;
    private final BookingBranchProfileService bookingBranchProfileService;
    private final BookingBranchBrandsService bookingBranchBrandsService;
    private final BookingIndividualCatalogService bookingIndividualCatalogService;
    private final BookingCarePackageCatalogService bookingCarePackageCatalogService;
    private final BookingPackagePriceInfoService bookingPackagePriceInfoService;

    @GetMapping("/api/v1/booking/partners")
    public BookingDiscoveryResponse discover(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return bookingDiscoveryService.discover(partnerId, q, lat, lng, radius, page, pageSize);
    }

    @GetMapping("/api/v1/booking/branches")
    public BookingBranchListResponse branches(
            @RequestHeader("Authorization") String token
    ) {
        return bookingDiscoveryService.listActive();
    }

    @GetMapping("/api/v1/booking/branches/{branchId}")
    public BookingBranchProfileResponse profile(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable Long branchId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        return bookingBranchProfileService.profile(branchId, lat, lng, acceptLanguage);
    }

    @GetMapping("/api/v1/booking/branches/{branchId}/brand-models")
    public BookingBrandModelsResponse brandModels(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable Long branchId
    ) {
        return bookingBranchBrandsService.list(branchId, acceptLanguage);
    }

    @GetMapping("/api/v1/booking/care-packages/{packageId}/price-info")
    public BookingPackagePriceInfoResponse packagePriceInfo(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable Long packageId
    ) {
        return bookingPackagePriceInfoService.info(packageId, acceptLanguage);
    }

    @GetMapping("/api/v1/booking/branches/{branchId}/care-packages")
    public BookingCarePackagesResponse carePackages(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable Long branchId
    ) {
        return bookingCarePackageCatalogService.list(branchId, acceptLanguage);
    }

    @GetMapping("/api/v1/booking/individual-service-filters")
    public BookingIndividualServiceFiltersResponse individualServiceFilters(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        return bookingIndividualCatalogService.filters(acceptLanguage);
    }

    @GetMapping("/api/v1/booking/branches/{branchId}/individual-services")
    public BookingIndividualServicesResponse individualServices(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable Long branchId,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Long carId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = carId == null ? null : parseUserId(userIdHeader);
        return bookingIndividualCatalogService.list(branchId, acceptLanguage, filter, carId, userId);
    }

    @GetMapping("/api/v1/booking/branches/{branchId}/catalog")
    public BookingCatalogResponse catalog(
            @RequestHeader("Authorization") String token,
            @PathVariable Long branchId
    ) {
        return bookingCatalogService.catalog(branchId);
    }

    @GetMapping("/api/v1/booking/branches/{branchId}/availability")
    public BookingAvailabilityResponse availability(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-Client-Timezone", required = false) String timezone,
            @PathVariable Long branchId,
            @RequestParam(required = false) String serviceKeys,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return bookingAvailabilityService.availability(branchId, serviceKeys, from, to, timezone);
    }

    /**
     * tr: Bu ayın günleri. date gün.ay.yıl, available true/false.
     * en: Days of this month. date is day.month.year, available is true or false.
     */
    @GetMapping("/api/v1/booking/branches/{branchId}/calendar")
    public BookingCalendarResponse calendar(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable Long branchId,
            @RequestBody BookingCalendarRequest request
    ) {
        return bookingCalendarService.month(branchId, request, acceptLanguage);
    }

    /**
     * tr: Seçilen günün saatleri. Aynı pencerede yeri çok olan, eşitse paket → hizmet → təmir.
     * en: Hours of the chosen day. Same window keeps the fullest range, then package, service, repair.
     */
    @GetMapping("/api/v1/booking/branches/{branchId}/ranges")
    public BookingDayRangesResponse ranges(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable Long branchId,
            @RequestBody BookingDayRangesRequest request
    ) {
        log.info("RANGES_GET body branchId={} date={} packageId={} individualServiceIds={} issuePresent={}",
                branchId,
                request == null ? null : request.getDate(),
                request == null ? null : request.getPackageId(),
                request == null ? null : request.getIndividualServiceIds(),
                request != null && request.getIssue() != null && !request.getIssue().isBlank());
        return bookingCalendarService.day(branchId, request, acceptLanguage);
    }

    private static Long parseUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw MissingFieldException.required("X-User-Id");
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw MissingFieldException.required("X-User-Id");
        }
    }
}
