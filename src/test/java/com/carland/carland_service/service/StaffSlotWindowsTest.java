package com.carland.carland_service.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffSlotWindowsTest {

    @Test
    void dropsLunchOverlapAndShortTailAndPastStart() {
        LocalDate day = LocalDate.of(2027, 6, 7);
        List<StaffSlotWindows.Slice> lunch = StaffSlotWindows.slices(
                day, LocalTime.of(9, 0), LocalTime.of(10, 0), 30,
                LocalTime.of(9, 30), LocalTime.of(10, 0), null);
        assertEquals(1, lunch.size());
        assertEquals(LocalTime.of(9, 0), lunch.get(0).start().toLocalTime());

        List<StaffSlotWindows.Slice> tail = StaffSlotWindows.slices(
                day, LocalTime.of(9, 0), LocalTime.of(10, 20), 45,
                null, null, null);
        assertEquals(1, tail.size());
        assertEquals(LocalTime.of(9, 45), tail.get(0).end().toLocalTime());

        OffsetDateTime now = ZonedDateTime.of(day, LocalTime.of(9, 15), StaffSlotWindows.ZONE).toOffsetDateTime();
        List<StaffSlotWindows.Slice> future = StaffSlotWindows.slices(
                day, LocalTime.of(9, 0), LocalTime.of(10, 0), 30,
                null, null, now);
        assertEquals(1, future.size());
        assertTrue(future.get(0).start().isAfter(now));
        assertEquals(LocalTime.of(9, 30), future.get(0).start().toLocalTime());
    }
}
