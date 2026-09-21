package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingQuoteResponse;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.BookingWriteRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchPackage;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchPackageRepository;
import com.carland.carland_service.repository.BranchServiceRepository;
import com.carland.carland_service.repository.RangeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCreateServiceTest {

    @Mock RangeRepository rangeRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingItemRepository bookingItemRepository;
    @Mock BranchPackageRepository packageRepository;
    @Mock BranchServiceRepository serviceRepository;

    BookingCreateService service;
    Range range;
    Branch branch;

    @BeforeEach
    void setUp() {
        service = new BookingCreateService(
                rangeRepository, bookingRepository, bookingItemRepository,
                packageRepository, serviceRepository, new ObjectMapper());
        Partner hyper = Partner.builder().id(1L).active(true).name("Hyper").source("hyper").build();
        branch = Branch.builder().id(7L).active(true).name("Xeqani").partner(hyper).build();
        Calendar calendar = Calendar.builder()
                .calendarId(8L)
                .day(LocalDate.of(2026, 10, 26))
                .branch(branch)
                .build();
        range = Range.builder()
                .rangeId(105L)
                .start(OffsetDateTime.parse("2026-10-26T05:00:00Z"))
                .end(OffsetDateTime.parse("2026-10-26T05:30:00Z"))
                .status("AVAILABLE")
                .workerCount(3)
                .bookingMode("approval")
                .serviceKey("pkg:hyper-extra")
                .appointments(new ArrayList<>())
                .calendar(calendar)
                .build();
    }

    @Test
    void quoteDoesNotPersist() {
        stubCatalogAndRange(false);
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);

        BookingQuoteResponse out = service.quote(request());

        assertEquals(12900, out.getPriceMin());
        assertEquals(105L, out.getSlotId());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createApprovalIsPending() {
        stubCatalogAndRange(true);
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.existsByRef(any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(44L);
            return b;
        });

        BookingView out = service.create(request(), 77L, "Asia/Baku");

        assertEquals("pending", out.getStatus());
        assertEquals("approval", out.getBookingMode());
        assertEquals("CC-", out.getRef().substring(0, 3));
        assertEquals("09:00", out.getStart());
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertEquals(77L, captor.getValue().getCustomerUserId());
    }

    @Test
    void packagePlusIncludedServiceConflicts() {
        stubCatalogAndRange(false);
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        BookingWriteRequest req = request();
        req.setServiceKeys(List.of("pkg:hyper-extra", "svc:oil-change"));
        when(serviceRepository.findByBranchIdAndActiveTrueOrderByIdAsc(7L)).thenReturn(List.of(
                com.carland.carland_service.entity.BranchService.builder()
                        .serviceKey("svc:oil-change")
                        .kind("SERVICE")
                        .titleJson("{\"en\":\"Oil\"}")
                        .priceMin(3500)
                        .priceMax(7500)
                        .active(true)
                        .build()
        ));

        assertThrows(ConflictException.class, () -> service.quote(req));
    }

    @Test
    void fullSlotConflicts() {
        when(rangeRepository.findByRangeId(105L)).thenReturn(range);
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(3L);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.quote(request()));
        assertEquals("capacity_full", ex.getMessage());
    }

    private void stubCatalogAndRange(boolean lock) {
        if (lock) {
            when(rangeRepository.lockByRangeId(105L)).thenReturn(Optional.of(range));
        } else {
            when(rangeRepository.findByRangeId(105L)).thenReturn(range);
        }
        when(packageRepository.findByBranchIdAndActiveTrueOrderByIdAsc(7L)).thenReturn(List.of(
                BranchPackage.builder()
                        .branch(branch)
                        .serviceKey("pkg:hyper-extra")
                        .titleJson("{\"en\":\"Hyper Extra\"}")
                        .priceMin(12900)
                        .priceMax(12900)
                        .includedServiceKeys("[\"svc:oil-change\",\"svc:air-filter\"]")
                        .active(true)
                        .build()
        ));
        when(serviceRepository.findByBranchIdAndActiveTrueOrderByIdAsc(7L)).thenReturn(List.of());
    }

    private BookingWriteRequest request() {
        return BookingWriteRequest.builder()
                .branchId(7L)
                .slotId(105L)
                .serviceKeys(List.of("pkg:hyper-extra"))
                .build();
    }
}
