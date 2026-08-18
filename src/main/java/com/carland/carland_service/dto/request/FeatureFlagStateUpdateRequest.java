package com.carland.carland_service.dto.request;

import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import lombok.Data;

@Data
public class FeatureFlagStateUpdateRequest {

    String version;
    Long flagId;
    UserRoles role;
    FeatureFlagState state;
}
