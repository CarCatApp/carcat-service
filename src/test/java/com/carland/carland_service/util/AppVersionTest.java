package com.carland.carland_service.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppVersionTest {

    @Test
    void numericCompareDoesNotUseStringOrder() {
        AppVersion newer = parse("3.0.10");
        AppVersion older = parse("3.0.5");
        assertTrue(newer.compareTo(older) > 0);
        assertTrue(older.compareTo(newer) < 0);
    }

    @Test
    void missingPatchEqualsExplicitZero() {
        assertEquals(parse("3.1"), parse("3.1.0"));
    }

    @Test
    void buildMetadataIsIgnored() {
        assertEquals(parse("3.0.5+76"), parse("3.0.5"));
    }

    @Test
    void preReleaseSuffixIsIgnored() {
        assertEquals(parse("3.0.5-beta"), parse("3.0.5"));
    }

    @Test
    void extraSegmentsAreIgnored() {
        assertEquals(parse("3.0.5.1"), parse("3.0.5"));
    }

    @Test
    void invalidInputIsUnknown() {
        assertTrue(AppVersion.parse("abc").isEmpty());
        assertTrue(AppVersion.parse("").isEmpty());
        assertTrue(AppVersion.parse("   ").isEmpty());
        assertTrue(AppVersion.parse(null).isEmpty());
        assertTrue(AppVersion.parse("3.x.1").isEmpty());
        assertTrue(AppVersion.parse("3.0.2147483648").isEmpty());
        assertTrue(AppVersion.parse("v3.0.5").isEmpty());
    }

    @Test
    void surroundingSpaceIsTrimmed() {
        assertEquals(parse("3.0.5"), parse("  3.0.5  "));
    }

    private static AppVersion parse(String raw) {
        Optional<AppVersion> parsed = AppVersion.parse(raw);
        assertTrue(parsed.isPresent(), raw);
        return parsed.get();
    }
}
