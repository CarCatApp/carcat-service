package com.carland.carland_service.dto.request;

import lombok.Data;

/**
 * tr: Yeni app version; her zaman is_current grid'ini kopyalar. copyFrom yok sayılır. makeCurrent default false.
 * en: New app version; always copies the is_current grid. copyFrom is ignored. makeCurrent defaults to false.
 */
@Data
public class FeatureFlagVersionCreateRequest {

    String semver;
    /** Ignored. Copy source is always the version marked current. */
    String copyFrom;
    boolean makeCurrent;
}
