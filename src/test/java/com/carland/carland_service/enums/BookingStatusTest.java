package com.carland.carland_service.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingStatusTest {

    @Test
    void autoAcceptedApiValue() {
        assertEquals("auto_accepted", BookingStatus.AUTO_ACCEPTED.apiValue());
    }

    @Test
    void occupyingIncludesAutoAcceptedAndStaffConfirmed() {
        assertTrue(BookingStatus.occupyingCapacity().contains("pending"));
        assertTrue(BookingStatus.occupyingCapacity().contains("confirmed"));
        assertTrue(BookingStatus.occupyingCapacity().contains("auto_accepted"));
        assertEquals(3, BookingStatus.occupyingCapacity().size());
    }
}
