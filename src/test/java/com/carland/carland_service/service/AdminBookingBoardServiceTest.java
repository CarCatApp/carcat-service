package com.carland.carland_service.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminBookingBoardServiceTest {

    @Test
    void bucketsMatchThePartnerBoard() {
        assertEquals(List.of("pending"), AdminBookingBoardService.statuses("pending"));
        assertEquals(List.of("confirmed", "auto_accepted"), AdminBookingBoardService.statuses("ready"));
        assertEquals(List.of("completed"), AdminBookingBoardService.statuses("completed"));
        assertTrue(AdminBookingBoardService.statuses("rejected").contains("no_show"));
        assertTrue(AdminBookingBoardService.statuses("rejected").contains("cancelled"));
        assertEquals("pending", AdminBookingBoardService.bucket(""));
        assertEquals("ready", AdminBookingBoardService.bucket("READY"));
    }
}
