package com.carland.carland_service.dto.request;

import lombok.Data;

@Data
public class FeatureFlagEndpointWriteRequest {

    String method;
    String path;
    Boolean neverGuard;
}
