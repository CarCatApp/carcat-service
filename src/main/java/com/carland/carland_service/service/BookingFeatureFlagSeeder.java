package com.carland.carland_service.service;

import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagRoleState;
import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.repository.FeatureFlagRepository;
import com.carland.carland_service.repository.FeatureFlagRoleStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * tr: Owner-app booking flag satırı. HIDDEN = 404 until PO/Aziz enables.
 * en: Owner-app booking flag row. HIDDEN = 404 until PO/Aziz enables.
 */
@Slf4j
@Component
@Order(15)
@RequiredArgsConstructor
public class BookingFeatureFlagSeeder implements ApplicationRunner {

    public static final String FLAG_NAME = "booking";

    private final FeatureFlagRepository featureFlagRepository;
    private final FeatureFlagRoleStateRepository roleStateRepository;

    @Override
    public void run(ApplicationArguments args) {
        FeatureFlag flag = featureFlagRepository.findByName(FLAG_NAME).orElse(null);
        if (flag == null) {
            LocalDateTime now = LocalDateTime.now();
            flag = featureFlagRepository.save(FeatureFlag.builder()
                    .name(FLAG_NAME)
                    .description("Owner-app booking APIs (CRCT-281+)")
                    .defaultState(FeatureFlagState.HIDDEN)
                    .minAvailableVersion("0.0.0")
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            log.info("BOOKING_FLAG_SEEDED name={}", FLAG_NAME);
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
            log.info("BOOKING_FLAG_ROLE_STATES_SEEDED name={}", FLAG_NAME);
        }
    }
}
