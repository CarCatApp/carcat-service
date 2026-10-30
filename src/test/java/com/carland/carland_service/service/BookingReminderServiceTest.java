package com.carland.carland_service.service;

import com.carland.carland_service.config.BookingReminderProperties;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingReminderServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingPushService bookingPushService;

    BookingReminderProperties properties;
    BookingReminderService service;

    @BeforeEach
    void setUp() {
        properties = new BookingReminderProperties();
        properties.setSlackMinutes(10);
        service = new BookingReminderService(properties, bookingRepository, bookingPushService);
    }

    @Test
    void tenMinuteSlackCentersTheWindows() {
        when(bookingRepository.findReadyForReminder(any(), any(), any())).thenReturn(List.of());

        service.sendDue(Instant.parse("2026-10-30T08:00:00Z"));

        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> to = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(bookingRepository, times(2)).findReadyForReminder(
                eq(BookingStatus.occupyingCapacity()), from.capture(), to.capture());
        assertEquals(OffsetDateTime.parse("2026-10-31T07:55:00Z"), from.getAllValues().get(0));
        assertEquals(OffsetDateTime.parse("2026-10-31T08:05:00Z"), to.getAllValues().get(0));
        assertEquals(OffsetDateTime.parse("2026-10-30T10:55:00Z"), from.getAllValues().get(1));
        assertEquals(OffsetDateTime.parse("2026-10-30T11:05:00Z"), to.getAllValues().get(1));
        verify(bookingPushService, never()).remindDayBefore(any());
        verify(bookingPushService, never()).remindSoon(any());
    }

    @Test
    void fifteenMinuteSlackUsesTheSameProperty() {
        properties.setSlackMinutes(15);
        when(bookingRepository.findReadyForReminder(any(), any(), any())).thenReturn(List.of());

        service.sendDue(Instant.parse("2026-10-30T08:00:00Z"));

        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> to = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(bookingRepository, times(2)).findReadyForReminder(any(), from.capture(), to.capture());
        assertEquals(OffsetDateTime.parse("2026-10-31T07:52:30Z"), from.getAllValues().get(0));
        assertEquals(OffsetDateTime.parse("2026-10-31T08:07:30Z"), to.getAllValues().get(0));
        assertEquals(OffsetDateTime.parse("2026-10-30T10:52:30Z"), from.getAllValues().get(1));
        assertEquals(OffsetDateTime.parse("2026-10-30T11:07:30Z"), to.getAllValues().get(1));
    }

    @Test
    void slackOutsideOneToSixtyFallsBackToTen() {
        properties.setSlackMinutes(90);
        when(bookingRepository.findReadyForReminder(any(), any(), any())).thenReturn(List.of());

        service.sendDue(Instant.parse("2026-10-30T08:00:00Z"));

        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(bookingRepository, times(2)).findReadyForReminder(any(), from.capture(), any());
        assertEquals(OffsetDateTime.parse("2026-10-31T07:55:00Z"), from.getAllValues().get(0));
        assertEquals(10, properties.resolvedSlackMinutes());
    }

    @Test
    void readyRowsArePushedAndAFailureDoesNotStopTheNext() {
        Booking first = Booking.builder().id(1L).build();
        Booking second = Booking.builder().id(2L).build();
        when(bookingRepository.findReadyForReminder(any(), any(), any()))
                .thenReturn(List.of(first, second))
                .thenReturn(List.of());
        doThrow(new RuntimeException("fcm")).when(bookingPushService).remindDayBefore(first);

        service.sendDue(Instant.parse("2026-10-30T08:00:00Z"));

        verify(bookingPushService).remindDayBefore(first);
        verify(bookingPushService).remindDayBefore(second);
        verify(bookingPushService, never()).remindSoon(any());
    }

    @Test
    void boundariesSitOnTheEpochAndSkipTheCurrentOne() {
        Duration ten = Duration.ofMinutes(10);
        assertEquals(Instant.parse("2026-10-30T08:10:00Z"),
                BookingReminderService.nextBoundary(Instant.parse("2026-10-30T08:00:00Z"), ten));
        assertEquals(Instant.parse("2026-10-30T08:10:00Z"),
                BookingReminderService.nextBoundary(Instant.parse("2026-10-30T08:03:00Z"), ten));
        assertEquals(Instant.parse("2026-10-30T08:20:00Z"),
                BookingReminderService.nextBoundary(Instant.parse("2026-10-30T08:10:00Z"), ten));

        Duration seven = Duration.ofMinutes(7);
        Instant now = Instant.parse("2026-10-30T08:00:00Z");
        Instant first = BookingReminderService.nextBoundary(now, seven);
        Instant second = BookingReminderService.nextBoundary(first, seven);
        assertEquals(Duration.ofMinutes(7), Duration.between(first, second));
        assertEquals(0, first.getEpochSecond() % seven.toSeconds());
        assertTrue(first.isAfter(now));
    }
}
