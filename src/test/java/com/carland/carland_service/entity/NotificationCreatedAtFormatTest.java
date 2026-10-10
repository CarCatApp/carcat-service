package com.carland.carland_service.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationCreatedAtFormatTest {

    @Test
    void createdAtIsLocalClockWithoutOffset() throws Exception {
        Notification row = Notification.builder()
                .id(1L)
                .created(LocalDate.of(2026, 10, 10))
                .createdAt(OffsetDateTime.of(2026, 10, 10, 23, 34, 8, 0, ZoneOffset.ofHours(4)))
                .type("BOOKING_REJECTED")
                .title("Rezervasiya rədd edildi")
                .notificationText("metin")
                .customerId(5L)
                .status("ACTIVE")
                .isRead(false)
                .build();
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = mapper.writeValueAsString(row);
        assertTrue(json.contains("\"createdAt\":\"23:34\""), json);
        assertTrue(!json.contains("+04:00"), json);
    }
}
