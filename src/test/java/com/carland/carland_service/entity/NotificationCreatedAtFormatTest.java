package com.carland.carland_service.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationCreatedAtFormatTest {

    @Test
    void utcInstantStillPrintsBakuClock() throws Exception {
        assertClock(OffsetDateTime.of(2026, 10, 10, 13, 44, 0, 0, ZoneOffset.UTC), "17:44");
    }

    @Test
    void bakuOffsetPrintsSameClock() throws Exception {
        assertClock(OffsetDateTime.of(2026, 10, 10, 17, 44, 0, 0, ZoneOffset.ofHours(4)), "17:44");
    }

    private static void assertClock(OffsetDateTime stored, String expected) throws Exception {
        Notification row = Notification.builder()
                .id(1L)
                .created(LocalDate.of(2026, 10, 10))
                .createdAt(stored)
                .type("BOOKING_ACCEPTED")
                .title("Rezervasiya təsdiqləndi")
                .notificationText("metin")
                .customerId(5L)
                .status("ACTIVE")
                .isRead(false)
                .build();
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setTimeZone(TimeZone.getTimeZone("UTC"));
        String json = mapper.writeValueAsString(row);
        assertTrue(json.contains("\"createdAt\":\"" + expected + "\""), json);
    }
}
