package com.carland.carland_service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PO: flag applies iff client SemVer ≥ minAvailableVersion.
 * Below min → omit from /me; interceptor treats as not present (pass).
 */
class FeatureFlagMinVersionGateTest {

    @Test
    void atOrAboveMinApplies() {
        assertTrue(SemVer.isAtLeast("2.1.5", "2.1.5"));
        assertTrue(SemVer.isAtLeast("2.1.10", "2.1.5"));
        assertTrue(SemVer.isAtLeast("3.0.0", "2.1.5"));
    }

    @Test
    void belowMinDoesNotApply() {
        assertFalse(SemVer.isAtLeast("2.1.4", "2.1.5"));
        assertFalse(SemVer.isAtLeast("2.0.0", "2.1.5"));
    }

    @Test
    void missingOrJunkClientDoesNotApply() {
        assertFalse(SemVer.isAtLeast(null, "2.1.5"));
        assertFalse(SemVer.isAtLeast("", "2.1.5"));
        assertFalse(SemVer.isAtLeast("app-2.x.y", "2.1.5"));
    }
}
