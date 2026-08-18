package com.carland.carland_service.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class FeatureFlagAttachRequest {

    List<Long> endpointIds;
}
