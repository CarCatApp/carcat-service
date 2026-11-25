package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPaidAmountServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock VisitRepository visitRepository;
    @InjectMocks BookingPaidAmountService service;

    @Test
    void earliestBookingStoresEverySameBranchAmount() {
        LocalDate day = LocalDate.of(2026, 11, 19);
        Branch branch = Branch.builder().id(13L).build();
        Calendar calendar = Calendar.builder().day(day).build();
        Booking early = booking(1L, branch, calendar, "2026-11-19T09:00:00Z");
        Booking late = booking(2L, branch, calendar, "2026-11-19T13:00:00Z");
        when(bookingRepository.findByBranchVinAndDay(13L, "KNAKU815BEA419818", day))
                .thenReturn(List.of(late, early));
        when(visitRepository.findPanelByBookingId(1L)).thenReturn(List.of());
        when(visitRepository.findPanelByBookingId(2L)).thenReturn(List.of());
        Car car = Car.builder().vin("KNAKU815BEA419818").build();
        when(visitRepository.findUnlinkedHyperByVinAndDay("KNAKU815BEA419818", day)).thenReturn(List.of(
                visit(car, "Xəqani Express", "208.10"),
                visit(car, "Babək Ekspress", "95.00"),
                visit(car, "Xəqani Express", "15.00")));

        service.syncHyperDay("KNAKU815BEA419818", day, 13L);

        assertEquals(List.of(new BigDecimal("208.10"), new BigDecimal("15.00")), early.getPaidAmounts());
        assertNull(late.getPaidAmounts());
    }

    @Test
    void panelBookingKeepsItsOwnAmount() {
        Branch branch = Branch.builder().id(13L).build();
        Calendar calendar = Calendar.builder().day(LocalDate.of(2026, 11, 19)).build();
        Booking booking = booking(5L, branch, calendar, "2026-11-19T09:00:00Z");
        Visit panel = Visit.builder().id(3L).source("panel").bookingId(5L)
                .finalCostAmount(new BigDecimal("40")).build();
        when(visitRepository.findPanelByBookingId(5L)).thenReturn(List.of(panel));

        service.syncBooking(booking);

        assertEquals(List.of(new BigDecimal("40.00")), booking.getPaidAmounts());
    }

    private static Booking booking(Long id, Branch branch, Calendar calendar, String start) {
        Range range = Range.builder().start(OffsetDateTime.parse(start)).calendar(calendar).build();
        return Booking.builder().id(id).vin("KNAKU815BEA419818").branch(branch).range(range).build();
    }

    private static Visit visit(Car car, String dealer, String amount) {
        return Visit.builder()
                .car(car)
                .serviceCenterId(1L)
                .dealer(dealer)
                .hyperRecordId(1L)
                .finalCostAmount(new BigDecimal(amount))
                .build();
    }
}
