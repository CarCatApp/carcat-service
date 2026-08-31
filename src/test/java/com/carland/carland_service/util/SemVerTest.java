package com.carland.carland_service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemVerTest {

    @Test
    void twoOneTenGreaterThanTwoOneFive() {
        assertTrue(cmp("2.1.10", "2.1.5") > 0);
        assertTrue(SemVer.isAtLeast("2.1.10", "2.1.5"));
    }

    @Test
    void twoTwoZeroGreaterThanTwoOneNinetyNine() {
        assertTrue(cmp("2.2.0", "2.1.99") > 0);
    }

    @Test
    void twoOneFiveGreaterThanTwoOneNormalized() {
        assertTrue(cmp("2.1.5", "2.1") > 0);
        assertEquals(1, SemVer.parse("2.1").minor());
        assertEquals(0, SemVer.parse("2.1").patch());
    }

    @Test
    void threeGreaterThanTwoNineNineNormalized() {
        assertTrue(cmp("3", "2.9.9") > 0);
        assertEquals(0, SemVer.parse("3").minor());
        assertEquals(0, SemVer.parse("3").patch());
    }

    @Test
    void stableGreaterThanBeta() {
        assertTrue(cmp("2.1.5", "2.1.5-beta") > 0);
    }

    @Test
    void beta2GreaterThanBeta1NumericPre() {
        assertTrue(cmp("2.1.5-beta.2", "2.1.5-beta.1") > 0);
    }

    @Test
    void vPrefixEqualsUnprefixed() {
        assertEquals(0, cmp("v2.1.5", "2.1.5"));
        assertEquals(SemVer.parse("v2.1.5"), SemVer.parse("2.1.5"));
    }

    @Test
    void buildMetadataIgnoredForCompareAndEquals() {
        assertEquals(0, cmp("2.1.5+build.20240101", "2.1.5"));
        assertEquals(SemVer.parse("2.1.5+build.20240101"), SemVer.parse("2.1.5"));
    }

    @Test
    void invalidInputDoesNotCrash() {
        assertTrue(SemVer.tryParse("app-2.x.y").isEmpty());
        assertTrue(SemVer.tryParse(null).isEmpty());
        assertTrue(SemVer.tryParse("").isEmpty());
        assertTrue(SemVer.tryParse("   ").isEmpty());
        assertThrows(SemVerFormatException.class, () -> SemVer.parse("app-2.x.y"));
        assertFalse(SemVer.isAtLeast("app-2.x.y", "2.1.0"));
        assertFalse(SemVer.isAtLeast("2.1.0", "not-a-version"));
        assertFalse(SemVer.isAtLeast(null, "2.1.0"));
    }

    @Test
    void isInRangeInclusive() {
        assertTrue(SemVer.isInRange("2.1.5", "2.1.0", "2.2.0"));
        assertTrue(SemVer.isInRange("2.1.0", "2.1.0", "2.2.0"));
        assertTrue(SemVer.isInRange("2.2.0", "2.1.0", "2.2.0"));
        assertFalse(SemVer.isInRange("2.0.9", "2.1.0", "2.2.0"));
        assertFalse(SemVer.isInRange("2.2.1", "2.1.0", "2.2.0"));
        assertFalse(SemVer.isInRange("bad", "2.1.0", "2.2.0"));
    }

    @Test
    void isAtMost() {
        assertTrue(SemVer.isAtMost("2.1.5", "2.1.5"));
        assertTrue(SemVer.isAtMost("2.1.4", "2.1.5"));
        assertFalse(SemVer.isAtMost("2.1.6", "2.1.5"));
    }

    @Test
    void numericPreReleaseLessThanAlphanumeric() {
        assertTrue(cmp("1.0.0-1", "1.0.0-alpha") < 0);
    }

    private static int cmp(String a, String b) {
        return SemVer.parse(a).compareTo(SemVer.parse(b));
    }
}
