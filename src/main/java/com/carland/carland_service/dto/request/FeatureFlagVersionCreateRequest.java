package com.carland.carland_service.dto.request;

import lombok.Data;

/**
 * tr: Yeni app version oluşturma; copyFrom semver'den rol state'lerini kopyalar.
 * en: Create a new app version; copies role states from copyFrom semver.
 */
@Data
public class FeatureFlagVersionCreateRequest {

    String semver;
    String copyFrom;
}
