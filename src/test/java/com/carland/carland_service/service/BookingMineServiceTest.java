package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingMineResponse;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingMineServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingItemRepository bookingItemRepository;

    BookingMineService service;
    Booking booking;

    @BeforeEach
    void setUp() {
        service = new BookingMineService(bookingRepository, bookingItemRepository);
        Partner hyper = Partner.builder().id(1L).name("Hyper").active(true).build();
        Branch branch = Branch.builder().id(7L).name("Xeqani").active(true).partner(hyper).build();
        Calendar calendar = Calendar.builder().day(LocalDate.of(2026, 10, 27)).branch(branch).build();
        Range range = Range.builder()
                .rangeId(87L)
                .start(OffsetDateTime.parse("2026-10-27T05:00:00Z"))
                .end(OffsetDateTime.parse("2026-10-27T05:30:00Z"))
                .bookingMode("instant")
                .calendar(calendar)
                .build();
        booking = Booking.builder()
                .id(3L)
                .ref("CC-147055")
                .customerUserId(54L)
                .status("auto_accepted")
                .carId(55L)
                .vin("3FA6P0HDXKR168752")
                .branch(branch)
                .range(range)
                .priceMin(12900)
                .priceMax(12900)
                .currency("AZN")
                .build();
    }

    @Test
    void mineReturnsOwnerBookingsWithStartsAt() {
        when(bookingRepository.findByCustomerUserId(eq(54L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingRepository.countGroupByStatus(54L))
                .thenReturn(List.<Object[]>of(new Object[]{"auto_accepted", 1L}));
        when(bookingItemRepository.findByBooking_IdIn(any())).thenReturn(List.of());

        BookingMineResponse out = service.mine(54L, null, null, 1, 20, null, "Asia/Baku");

        assertEquals(1, out.getItems().size());
        assertEquals("CC-147055", out.getItems().get(0).getRef());
        assertEquals("auto_accepted", out.getItems().get(0).getStatus());
        assertEquals(55L, out.getItems().get(0).getCarId());
        assertEquals("Xeqani", out.getItems().get(0).getBranchName());
        assertEquals("Hyper", out.getItems().get(0).getPartnerName());
        assertEquals("09:00", out.getItems().get(0).getStart());
        assertEquals("2026-10-27T09:00:00+04:00", out.getItems().get(0).getStartsAt());
        assertEquals(1L, out.getCounts().get("auto_accepted"));
        assertEquals(0L, out.getCounts().get("pending"));
        assertEquals(1, out.getTotal());
    }

    @Test
    void confirmedFilterAlsoLoadsAutoAccepted() {
        when(bookingRepository.findByCustomerUserIdAndStatusIn(eq(54L), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingRepository.countGroupByStatus(54L)).thenReturn(List.of());
        when(bookingItemRepository.findByBooking_IdIn(any())).thenReturn(List.of());

        service.mine(54L, "pending,confirmed", null, 1, 20, null, "Asia/Baku");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(bookingRepository).findByCustomerUserIdAndStatusIn(eq(54L), captor.capture(), any(Pageable.class));
        assertTrue(captor.getValue().contains("pending"));
        assertTrue(captor.getValue().contains("confirmed"));
        assertTrue(captor.getValue().contains("auto_accepted"));
    }

    @Test
    void carIdFiltersList() {
        when(bookingRepository.findByCustomerUserIdAndCarId(eq(54L), eq(55L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingRepository.countGroupByStatusAndCarId(54L, 55L)).thenReturn(List.of());
        when(bookingItemRepository.findByBooking_IdIn(any())).thenReturn(List.of());

        BookingMineResponse out = service.mine(54L, null, 55L, 1, 20, null, "Asia/Baku");

        assertEquals(1, out.getItems().size());
        verify(bookingRepository).findByCustomerUserIdAndCarId(eq(54L), eq(55L), any(Pageable.class));
    }

    @Test
    void requiresUserId() {
        assertThrows(MissingFieldException.class,
                () -> service.mine(null, null, null, 1, 20, null, "Asia/Baku"));
    }

    @Test
    void parseCanceledAlias() {
        List<String> statuses = BookingMineService.parseStatuses("canceled");
        assertEquals(List.of("cancelled"), statuses);
    }
}
