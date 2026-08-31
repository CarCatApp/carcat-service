package com.carland.carland_service.dto.request;

import com.carland.carland_service.enums.FeatureFlagState;
import lombok.Data;

@Data
public class FeatureFlagWriteRequest {

    String name;
    String description;
    FeatureFlagState defaultState;
    /** SemVer: flag applies to clients ≥ this version. Required on create. */
    String minAvailableVersion;
}
