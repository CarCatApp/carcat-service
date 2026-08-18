package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeatureFlagEndpointView {

    Long id;
    String method;
    String path;
}
