package com.carland.carland_service.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingModeTest {

    @Test
    void blankDefaultsToInstant() {
        assertEquals("instant", BookingMode.normalizeOrDefault(null));
        assertEquals("instant", BookingMode.normalizeOrDefault("  "));
    }

    @Test
    void acceptsInstantAndApproval() {
        assertEquals("approval", BookingMode.normalizeOrDefault("APPROVAL"));
        assertEquals("instant", BookingMode.normalizeOrDefault("instant"));
    }

    @Test
    void rejectsUnknown() {
        assertNull(BookingMode.normalizeOrDefault("hyper"));
    }
}
