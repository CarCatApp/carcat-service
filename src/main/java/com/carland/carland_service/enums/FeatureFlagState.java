package com.carland.carland_service.enums;

/**
 * tr: Feature flag görünürlük durumu; ENABLED geçiş, DISABLED 403, HIDDEN 404.
 * en: Feature-flag visibility; ENABLED passes, DISABLED returns 403, HIDDEN returns 404.
 */
public enum FeatureFlagState {
    ENABLED,
    DISABLED,
    HIDDEN
}
