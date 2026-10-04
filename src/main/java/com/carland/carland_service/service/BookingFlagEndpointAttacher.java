package com.carland.carland_service.service;

import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagEndpoint;
import com.carland.carland_service.entity.FeatureFlagRoleState;
import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.repository.FeatureFlagEndpointRepository;
import com.carland.carland_service.repository.FeatureFlagRepository;
import com.carland.carland_service.repository.FeatureFlagRoleStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tr: Owner booking path'lerini booking flag'e bağlar (scanner'dan sonra). State'i açmaz.
 * en: Attaches owner booking paths to the booking flag after scan. Does not enable the flag.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingFlagEndpointAttacher {

    record OwnerRoute(String method, String path) {}

    static final String DISCOVER_PATH = "/api/v1/booking/partners";
    static final String CATALOG_PATH = "/api/v1/booking/branches/{branchId}/catalog";
    static final String BRAND_MODELS_PATH = "/api/v1/booking/branches/{branchId}/brand-models";
    static final String INDIVIDUAL_SERVICE_FILTERS_PATH = "/api/v1/booking/individual-service-filters";
    static final String INDIVIDUAL_SERVICES_PATH = "/api/v1/booking/branches/{branchId}/individual-services";
    static final String PACKAGE_PRICE_INFO_PATH = "/api/v1/booking/care-packages/{packageId}/price-info";
    static final String CARE_PACKAGES_PATH = "/api/v1/booking/branches/{branchId}/care-packages";
    static final String AVAILABILITY_PATH = "/api/v1/booking/branches/{branchId}/availability";
    static final String CALENDAR_PATH = "/api/v1/booking/branches/{branchId}/calendar";
    static final String RANGES_PATH = "/api/v1/booking/branches/{branchId}/ranges";
    static final String APPOINTMENTS_PATH = "/api/v1/booking/branches/{branchId}/appointments";
    static final String QUOTE_PATH = "/api/v1/booking/bookings/quote";
    static final String CREATE_PATH = "/api/v1/booking/bookings";
    static final String MINE_PATH = "/api/v1/booking/bookings/mine";
    static final String DETAIL_PATH = "/api/v1/booking/bookings/{bookingId}";
    static final String CANCEL_PATH = "/api/v1/booking/bookings/{bookingId}/cancel";
    static final String CANCEL_REASONS_PATH = "/api/v1/booking/cancel-reasons";
    static final String CANCEL_REASONS_ALIAS = "/api/v1/booking/bookings/cancel-reasons";
    static final String RATING_PATH = "/api/v1/booking/branches/{branchId}/ratings";
    static final List<OwnerRoute> OWNER_ROUTES = List.of(
            new OwnerRoute("GET", DISCOVER_PATH),
            new OwnerRoute("GET", CATALOG_PATH),
            new OwnerRoute("GET", BRAND_MODELS_PATH),
            new OwnerRoute("GET", INDIVIDUAL_SERVICE_FILTERS_PATH),
            new OwnerRoute("GET", INDIVIDUAL_SERVICES_PATH),
            new OwnerRoute("GET", PACKAGE_PRICE_INFO_PATH),
            new OwnerRoute("GET", CARE_PACKAGES_PATH),
            new OwnerRoute("GET", AVAILABILITY_PATH),
            new OwnerRoute("GET", CALENDAR_PATH),
            new OwnerRoute("GET", RANGES_PATH),
            new OwnerRoute("POST", APPOINTMENTS_PATH),
            new OwnerRoute("POST", QUOTE_PATH),
            new OwnerRoute("POST", CREATE_PATH),
            new OwnerRoute("GET", CREATE_PATH),
            new OwnerRoute("GET", MINE_PATH),
            new OwnerRoute("GET", DETAIL_PATH),
            new OwnerRoute("PATCH", DETAIL_PATH),
            new OwnerRoute("GET", CANCEL_REASONS_PATH),
            new OwnerRoute("GET", CANCEL_REASONS_ALIAS),
            new OwnerRoute("POST", CANCEL_PATH),
            new OwnerRoute("POST", RATING_PATH)
    );

    private final FeatureFlagRepository flagRepository;
    private final FeatureFlagEndpointRepository endpointRepository;
    private final FeatureFlagRoleStateRepository roleStateRepository;
    private final FeatureFlagService featureFlagService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void attachDiscover() {
        FeatureFlag flag = flagRepository.findByName(BookingFeatureFlagSeeder.FLAG_NAME).orElse(null);
        boolean created = false;
        if (flag == null) {
            LocalDateTime now = LocalDateTime.now();
            flag = flagRepository.save(FeatureFlag.builder()
                    .name(BookingFeatureFlagSeeder.FLAG_NAME)
                    .description("Owner-app booking APIs (CRCT-281+)")
                    .defaultState(FeatureFlagState.HIDDEN)
                    .minAvailableVersion("0.0.0")
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            created = true;
            log.info("BOOKING_FLAG_SEEDED name={}", flag.getName());
        }
        Map<String, FeatureFlagEndpoint> byKey = new HashMap<>();
        for (FeatureFlagEndpoint endpoint : endpointRepository.findAllWithFlag()) {
            byKey.put(endpoint.getHttpMethod() + " " + endpoint.getPathPattern(), endpoint);
        }
        boolean attached = false;
        for (OwnerRoute route : OWNER_ROUTES) {
            FeatureFlagEndpoint endpoint = byKey.get(route.method() + " " + route.path());
            if (endpoint != null && sameFlag(endpoint, flag)) {
                continue;
            }
            endpoint = featureFlagService.upsertEndpoint(route.method(), route.path(), false);
            if (!sameFlag(endpoint, flag)) {
                endpoint.setFlag(flag);
                endpointRepository.save(endpoint);
                attached = true;
                log.info("BOOKING_FLAG_ATTACHED method={} path={}", route.method(), route.path());
            }
        }
        boolean rolesSeeded = false;
        if (!roleStateRepository.existsByFlag(flag)) {
            FeatureFlagState state = flag.getDefaultState() == null ? FeatureFlagState.HIDDEN : flag.getDefaultState();
            for (UserRoles role : UserRoles.values()) {
                roleStateRepository.save(FeatureFlagRoleState.builder()
                        .flag(flag)
                        .role(role)
                        .state(state)
                        .build());
            }
            rolesSeeded = true;
            log.info("BOOKING_FLAG_ROLE_STATES_SEEDED name={}", flag.getName());
        }
        if (created || attached || rolesSeeded) {
            featureFlagService.reloadCache();
        }
    }

    private static boolean sameFlag(FeatureFlagEndpoint endpoint, FeatureFlag flag) {
        return endpoint.getFlag() != null
                && flag.getId() != null
                && flag.getId().equals(endpoint.getFlag().getId());
    }
}
