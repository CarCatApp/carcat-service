package com.carland.carland_service.service.impl;

import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagRoleState;
import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.repository.FeatureFlagAuditRepository;
import com.carland.carland_service.repository.FeatureFlagEndpointRepository;
import com.carland.carland_service.repository.FeatureFlagRepository;
import com.carland.carland_service.repository.FeatureFlagRoleStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicPromoFlagTest {

    @Mock FeatureFlagRepository flagRepository;
    @Mock FeatureFlagEndpointRepository endpointRepository;
    @Mock FeatureFlagRoleStateRepository roleStateRepository;
    @Mock FeatureFlagAuditRepository auditRepository;

    FeatureFlagServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FeatureFlagServiceImpl(flagRepository, endpointRepository, roleStateRepository, auditRepository);
    }

    @Test
    void missingFlagIsHidden() {
        when(flagRepository.findByName(FeatureFlagServiceImpl.PROMO_CODE)).thenReturn(Optional.empty());

        assertEquals(FeatureFlagState.HIDDEN, service.publicPromo("1.0.0").getState());
    }

    @Test
    void userEnabledStateIsReturned() {
        FeatureFlag flag = FeatureFlag.builder().id(1L).name(FeatureFlagServiceImpl.PROMO_CODE)
                .minAvailableVersion("1.0.0").build();
        when(flagRepository.findByName(FeatureFlagServiceImpl.PROMO_CODE)).thenReturn(Optional.of(flag));
        when(roleStateRepository.findFirstByFlagAndRole(flag, UserRoles.USER)).thenReturn(Optional.of(
                FeatureFlagRoleState.builder().flag(flag).role(UserRoles.USER).state(FeatureFlagState.ENABLED).build()));

        assertEquals(FeatureFlagState.ENABLED, service.publicPromo("1.2.0").getState());
        assertEquals(FeatureFlagServiceImpl.PROMO_CODE, service.publicPromo("1.2.0").getName());
    }

    @Test
    void aLowAppVersionIsHidden() {
        FeatureFlag flag = FeatureFlag.builder().id(1L).name(FeatureFlagServiceImpl.PROMO_CODE)
                .minAvailableVersion("2.0.0").build();
        when(flagRepository.findByName(FeatureFlagServiceImpl.PROMO_CODE)).thenReturn(Optional.of(flag));

        assertEquals(FeatureFlagState.HIDDEN, service.publicPromo("1.0.0").getState());
    }
}
