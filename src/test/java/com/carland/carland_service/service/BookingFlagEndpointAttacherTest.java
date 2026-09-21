package com.carland.carland_service.service;

import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagEndpoint;
import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.repository.FeatureFlagEndpointRepository;
import com.carland.carland_service.repository.FeatureFlagRepository;
import com.carland.carland_service.repository.FeatureFlagRoleStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingFlagEndpointAttacherTest {

    @Mock FeatureFlagRepository flagRepository;
    @Mock FeatureFlagEndpointRepository endpointRepository;
    @Mock FeatureFlagRoleStateRepository roleStateRepository;
    @Mock FeatureFlagService featureFlagService;
    @InjectMocks BookingFlagEndpointAttacher attacher;

    @Test
    void attachesDiscoverWhenUnclaimed() {
        FeatureFlag flag = FeatureFlag.builder().id(3L).name("booking").defaultState(FeatureFlagState.HIDDEN).build();
        FeatureFlagEndpoint ep = FeatureFlagEndpoint.builder()
                .id(11L)
                .httpMethod("GET")
                .pathPattern(BookingFlagEndpointAttacher.DISCOVER_PATH)
                .neverGuard(false)
                .build();
        when(flagRepository.findByName("booking")).thenReturn(Optional.of(flag));
        when(featureFlagService.upsertEndpoint("GET", BookingFlagEndpointAttacher.DISCOVER_PATH, false)).thenReturn(ep);
        when(roleStateRepository.existsByFlag(flag)).thenReturn(true);

        attacher.attachDiscover();

        verify(endpointRepository).save(ep);
        verify(featureFlagService).reloadCache();
    }

    @Test
    void skipsSaveWhenAlreadyAttached() {
        FeatureFlag flag = FeatureFlag.builder().id(3L).name("booking").build();
        FeatureFlagEndpoint ep = FeatureFlagEndpoint.builder()
                .flag(flag)
                .httpMethod("GET")
                .pathPattern(BookingFlagEndpointAttacher.DISCOVER_PATH)
                .build();
        when(flagRepository.findByName("booking")).thenReturn(Optional.of(flag));
        when(featureFlagService.upsertEndpoint(eq("GET"), eq(BookingFlagEndpointAttacher.DISCOVER_PATH), eq(false)))
                .thenReturn(ep);
        when(roleStateRepository.existsByFlag(flag)).thenReturn(true);

        attacher.attachDiscover();

        verify(endpointRepository, never()).save(any());
        verify(featureFlagService).reloadCache();
    }
}
