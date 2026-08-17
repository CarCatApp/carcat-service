package com.carland.carland_service.dto.request;

import lombok.Data;

/**
 * tr: Yeni app version oluşturma; copyFrom semver'den rol state'lerini kopyalar. makeCurrent default false.
 * en: Create a new app version; copies role states from copyFrom. makeCurrent defaults to false.
 */
@Data
public class FeatureFlagVersionCreateRequest {

    String semver;
    String copyFrom;
    boolean makeCurrent;
}
