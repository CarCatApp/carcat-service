package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingInboxResponse;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingItem;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffBookingServiceTest {

    @Mock BookingStaffAccess bookingStaffAccess;
    @Mock BookingRepository bookingRepository;
    @Mock BookingItemRepository bookingItemRepository;

    StaffBookingService service;
    BookingStaff staff;
    Branch branch;
    Booking booking;

    @BeforeEach
    void setUp() {
        service = new StaffBookingService(bookingStaffAccess, bookingRepository, bookingItemRepository);
        Partner hyper = Partner.builder().id(1L).name("Hyper").active(true).build();
        branch = Branch.builder().id(7L).name("Xeqani").active(true).partner(hyper).build();
        staff = BookingStaff.builder()
                .userId(9L)
                .role(BookingStaffRole.PARTNER_ADMIN.name())
                .status("ACTIVE")
                .partner(hyper)
                .build();
        Calendar calendar = Calendar.builder().day(LocalDate.of(2026, 10, 26)).branch(branch).build();
        Range range = Range.builder()
                .rangeId(105L)
                .start(OffsetDateTime.parse("2026-10-26T05:00:00Z"))
                .end(OffsetDateTime.parse("2026-10-26T05:30:00Z"))
                .bookingMode("approval")
                .calendar(calendar)
                .build();
        booking = Booking.builder()
                .id(1L)
                .ref("CC-959345")
                .status("pending")
                .branch(branch)
                .range(range)
                .priceMin(12900)
                .priceMax(12900)
                .currency("AZN")
                .build();
    }

    @Test
    void inboxReturnsPending() {
        when(bookingStaffAccess.requireActive(9L, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 7L, "az")).thenReturn(branch);
        when(bookingRepository.findByBranch_IdAndStatusOrderByCreatedAtDesc(eq(7L), eq("pending"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingItemRepository.findByBooking_IdIn(List.of(1L))).thenReturn(List.of(
                BookingItem.builder().booking(booking).serviceKey("pkg:hyper-extra").build()));

        BookingInboxResponse out = service.inbox(9L, false, "pending", 7L, 1, 20, "Asia/Baku", "az");

        assertEquals(1, out.getItems().size());
        assertEquals("CC-959345", out.getItems().get(0).getRef());
        assertEquals("pkg:hyper-extra", out.getItems().get(0).getServiceKeys().get(0));
    }

    @Test
    void acceptConfirmsPending() {
        when(bookingStaffAccess.requireActive(9L, "az")).thenReturn(staff);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingStaffAccess.requireWritableBranch(staff, 7L, "az")).thenReturn(branch);
        when(bookingItemRepository.findByBooking_IdOrderByIdAsc(1L)).thenReturn(List.of(
                BookingItem.builder().booking(booking).serviceKey("pkg:hyper-extra").build()));

        BookingView out = service.accept(9L, false, 1L, "Asia/Baku", "az");

        assertEquals("confirmed", out.getStatus());
        assertEquals("confirmed", booking.getStatus());
        verify(bookingStaffAccess).requireWritableBranch(staff, 7L, "az");
    }

    @Test
    void acceptNonPendingConflicts() {
        booking.setStatus("confirmed");
        when(bookingStaffAccess.requireActive(9L, "az")).thenReturn(staff);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingStaffAccess.requireWritableBranch(staff, 7L, "az")).thenReturn(branch);

        assertThrows(ConflictException.class, () -> service.accept(9L, false, 1L, "Asia/Baku", "az"));
    }
}
