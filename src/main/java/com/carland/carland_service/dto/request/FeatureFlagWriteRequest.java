package com.carland.carland_service.dto.request;

import com.carland.carland_service.enums.FeatureFlagState;
import lombok.Data;

@Data
public class FeatureFlagWriteRequest {

    String name;
    String description;
    FeatureFlagState defaultState;
    /** Optional semver to seed. Blank → current version only (not all versions). */
    String version;
}
