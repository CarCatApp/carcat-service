package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCapacityServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingPushService bookingPushService;
    @InjectMocks BookingCapacityService service;

    @Test
    void fullRangeRejectsTheWaitingBookings() {
        Range range = Range.builder().rangeId(8L).workerCount(2).build();
        Booking first = Booking.builder().id(1L).status("pending").build();
        Booking second = Booking.builder().id(2L).status("pending").build();
        Booking third = Booking.builder().id(3L).status("pending").build();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(eq(8L), any())).thenReturn(2L);
        when(bookingRepository.findByRange_RangeIdAndStatus(8L, "pending"))
                .thenReturn(List.of(first, second, third));

        service.closePendingWhenFull(range);

        assertEquals("rejected", first.getStatus());
        assertEquals("rejected", second.getStatus());
        assertEquals("rejected", third.getStatus());
        assertEquals(BookingCapacityService.PLACES_FULL, first.getCancelReasonCode());
        assertEquals("Yerlər dolduğu üçün rezervasiyanızı qəbul edə bilmədik.", first.getCancelNote());
        verify(bookingPushService).rejected(first);
        verify(bookingPushService).rejected(second);
        verify(bookingPushService).rejected(third);
    }

    @Test
    void pushFailureStillRejectsWaitingBookings() {
        Range range = Range.builder().rangeId(8L).workerCount(1).build();
        Booking waiting = Booking.builder().id(1L).status("pending").build();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(eq(8L), any())).thenReturn(1L);
        when(bookingRepository.findByRange_RangeIdAndStatus(8L, "pending")).thenReturn(List.of(waiting));
        doThrow(new RuntimeException("fcm")).when(bookingPushService).rejected(waiting);

        service.closePendingWhenFull(range);

        assertEquals("rejected", waiting.getStatus());
        assertEquals(BookingCapacityService.PLACES_FULL, waiting.getCancelReasonCode());
    }

    @Test
    void openRangeLeavesWaitingBookings() {
        Range range = Range.builder().rangeId(8L).workerCount(2).build();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(eq(8L), any())).thenReturn(1L);

        service.closePendingWhenFull(range);

        verify(bookingPushService, never()).rejected(any());
    }

    @Test
    void acceptedBookingsBlockAnotherAcceptance() {
        Range range = Range.builder().rangeId(8L).workerCount(2).build();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(eq(8L), any())).thenReturn(2L);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.requirePlace(range, "az"));

        assertEquals("Yerlər dolduğu üçün rezervasiyanızı qəbul edə bilmədik.", ex.getMessage());
    }
}
