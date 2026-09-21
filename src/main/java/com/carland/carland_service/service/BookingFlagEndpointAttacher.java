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

    static final String DISCOVER_PATH = "/api/v1/booking/partners";
    static final String CATALOG_PATH = "/api/v1/booking/branches/{branchId}/catalog";
    static final List<String> OWNER_GET_PATHS = List.of(DISCOVER_PATH, CATALOG_PATH);

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
        for (String path : OWNER_GET_PATHS) {
            FeatureFlagEndpoint endpoint = featureFlagService.upsertEndpoint("GET", path, false);
            if (endpoint.getFlag() == null || !BookingFeatureFlagSeeder.FLAG_NAME.equals(endpoint.getFlag().getName())) {
                endpoint.setFlag(flag);
                endpointRepository.save(endpoint);
                log.info("BOOKING_FLAG_ATTACHED method=GET path={}", path);
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
