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

import java.util.List;

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
    static final String AVAILABILITY_PATH = "/api/v1/booking/branches/{branchId}/availability";
    static final String QUOTE_PATH = "/api/v1/booking/bookings/quote";
    static final String CREATE_PATH = "/api/v1/booking/bookings";
    static final String MINE_PATH = "/api/v1/booking/bookings/mine";
    static final String DETAIL_PATH = "/api/v1/booking/bookings/{bookingId}";
    static final String CANCEL_PATH = "/api/v1/booking/bookings/{bookingId}/cancel";
    static final String CANCEL_REASONS_PATH = "/api/v1/booking/cancel-reasons";
    static final String CANCEL_REASONS_ALIAS = "/api/v1/booking/bookings/cancel-reasons";
    static final List<OwnerRoute> OWNER_ROUTES = List.of(
            new OwnerRoute("GET", DISCOVER_PATH),
            new OwnerRoute("GET", CATALOG_PATH),
            new OwnerRoute("GET", AVAILABILITY_PATH),
            new OwnerRoute("POST", QUOTE_PATH),
            new OwnerRoute("POST", CREATE_PATH),
            new OwnerRoute("GET", CREATE_PATH),
            new OwnerRoute("GET", MINE_PATH),
            new OwnerRoute("GET", DETAIL_PATH),
            new OwnerRoute("PATCH", DETAIL_PATH),
            new OwnerRoute("GET", CANCEL_REASONS_PATH),
            new OwnerRoute("GET", CANCEL_REASONS_ALIAS),
            new OwnerRoute("POST", CANCEL_PATH)
    );

    private final FeatureFlagRepository flagRepository;
    private final FeatureFlagEndpointRepository endpointRepository;
    private final FeatureFlagRoleStateRepository roleStateRepository;
    private final FeatureFlagService featureFlagService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void attachDiscover() {
        FeatureFlag flag = flagRepository.findByName(BookingFeatureFlagSeeder.FLAG_NAME).orElse(null);
        if (flag == null) {
            log.info("BOOKING_FLAG_ATTACH_SKIP flag missing");
            return;
        }
        for (OwnerRoute route : OWNER_ROUTES) {
            FeatureFlagEndpoint endpoint = featureFlagService.upsertEndpoint(route.method(), route.path(), false);
            if (endpoint.getFlag() == null || !BookingFeatureFlagSeeder.FLAG_NAME.equals(endpoint.getFlag().getName())) {
                endpoint.setFlag(flag);
                endpointRepository.save(endpoint);
                log.info("BOOKING_FLAG_ATTACHED method={} path={}", route.method(), route.path());
            }
        }
        if (!roleStateRepository.existsByFlag(flag)) {
            FeatureFlagState state = flag.getDefaultState() == null ? FeatureFlagState.HIDDEN : flag.getDefaultState();
            for (UserRoles role : UserRoles.values()) {
                roleStateRepository.save(FeatureFlagRoleState.builder()
                        .flag(flag)
                        .role(role)
                        .state(state)
                        .build());
            }
            log.info("BOOKING_FLAG_ROLE_STATES_SEEDED name={}", flag.getName());
        }
        featureFlagService.reloadCache();
    }
}
