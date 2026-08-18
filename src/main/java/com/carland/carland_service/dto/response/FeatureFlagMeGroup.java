package com.carland.carland_service.dto.response;

import com.carland.carland_service.enums.FeatureFlagState;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class FeatureFlagMeGroup {

    FeatureFlagState state;
    List<FeatureFlagEndpointView> endpoints;
}
