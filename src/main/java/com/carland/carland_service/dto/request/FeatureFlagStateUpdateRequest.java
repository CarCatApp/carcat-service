package com.carland.carland_service.dto.request;

import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import lombok.Data;

/**
 * tr: Admin grid'de bir hücre (endpoint × rol) durumunu güncelleme isteği.
 * en: Admin-grid request to update one cell (endpoint × role) state.
 */
@Data
public class FeatureFlagStateUpdateRequest {

    String version;
    String method;
    String path;
    UserRoles role;
    FeatureFlagState state;
}
