package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingDetailResponse;
import com.carland.carland_service.dto.booking.BookingMineResponse;
import com.carland.carland_service.dto.booking.BookingPatchRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingItem;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CarRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingMineServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingItemRepository bookingItemRepository;
    @Mock CarRepository carRepository;
    @Mock BookingCreateService bookingCreateService;

    BookingMineService service;
    Booking booking;
    Branch branch;

    @BeforeEach
    void setUp() {
        service = new BookingMineService(bookingRepository, bookingItemRepository, carRepository, bookingCreateService);
        Partner hyper = Partner.builder().id(1L).name("Hyper").active(true).build();
        branch = Branch.builder().id(7L).name("Xeqani").address("Xeqani").active(true).partner(hyper).build();
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
        stubOwnedCar(55L, 54L);
        when(bookingRepository.findByCustomerUserIdAndCarId(eq(54L), eq(55L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingRepository.countGroupByStatusAndCarId(54L, 55L)).thenReturn(List.of());
        when(bookingItemRepository.findByBooking_IdIn(any())).thenReturn(List.of());

        BookingMineResponse out = service.mine(54L, null, 55L, 1, 20, null, "Asia/Baku");

        assertEquals(1, out.getItems().size());
        verify(bookingRepository).findByCustomerUserIdAndCarId(eq(54L), eq(55L), any(Pageable.class));
    }

    @Test
    void foreignCarIdIsForbidden() {
        stubOwnedCar(99L, 12L);
        assertThrows(ForbiddenException.class, () -> service.mine(54L, null, 99L, 1, 20, null, "Asia/Baku"));
    }

    @Test
    void unknownCarIdIsNotFound() {
        when(carRepository.findByCarId(404L)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> service.mine(54L, null, 404L, 1, 20, null, "Asia/Baku"));
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

    @Test
    void detailByIdReturnsCarAndLines() {
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking));
        when(bookingItemRepository.findByBooking_IdOrderByIdAsc(3L)).thenReturn(List.of(
                BookingItem.builder()
                        .serviceKey("pkg:hyper-extra")
                        .titleSnapshot("Hyper Extra")
                        .priceMin(12900)
                        .priceMax(12900)
                        .build()
        ));
        when(carRepository.findByCarId(55L)).thenReturn(Car.builder()
                .carId(55L)
                .vin("3FA6P0HDXKR168752")
                .brand("BMW")
                .model("3 Series")
                .modelYear(2019)
                .build());

        BookingDetailResponse out = service.detail(54L, "3", "Asia/Baku");

        assertEquals("CC-147055", out.getRef());
        assertEquals("auto_accepted", out.getStatus());
        assertEquals("BMW", out.getCar().getBrand());
        assertEquals(2019, out.getCar().getYear());
        assertEquals("Hyper Extra", out.getItems().get(0).getTitle());
        assertEquals("Xeqani", out.getBranchAddress());
        assertEquals("2026-10-27T09:00:00+04:00", out.getStartsAt());
    }

    @Test
    void detailByRef() {
        when(bookingRepository.findByRef("CC-147055")).thenReturn(Optional.of(booking));
        when(bookingItemRepository.findByBooking_IdOrderByIdAsc(3L)).thenReturn(List.of());
        when(carRepository.findByCarId(55L)).thenReturn(null);

        BookingDetailResponse out = service.detail(54L, "cc-147055", "Asia/Baku");

        assertEquals(3L, out.getBookingId());
        assertEquals(55L, out.getCar().getCarId());
    }

    @Test
    void detailForbiddenForOtherOwner() {
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking));
        assertThrows(ForbiddenException.class, () -> service.detail(99L, "3", "Asia/Baku"));
    }

    @Test
    void detailMissingIsNotFound() {
        when(bookingRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.detail(54L, "9", "Asia/Baku"));
    }

    @Test
    void patchRejectedCannotEdit() {
        booking.setStatus("rejected");
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking));
        assertThrows(ConflictException.class, () -> service.patch(
                54L, "3", BookingPatchRequest.builder().slotId(88L).build(), "Asia/Baku"));
        verify(bookingCreateService, never()).applyEdit(any(), any(), any());
    }

    @Test
    void patchCompletedCannotEdit() {
        booking.setStatus("completed");
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking));
        ConflictException ex = assertThrows(ConflictException.class, () -> service.patch(
                54L, "3", BookingPatchRequest.builder().slotId(88L).build(), "Asia/Baku"));
        assertEquals("Completed bookings cannot be edited", ex.getMessage());
    }

    @Test
    void patchMovesSlot() {
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking));
        when(bookingItemRepository.findByBooking_IdOrderByIdAsc(3L)).thenReturn(List.of());
        when(carRepository.findByCarId(55L)).thenReturn(null);

        BookingDetailResponse out = service.patch(
                54L, "3", BookingPatchRequest.builder().slotId(88L).build(), "Asia/Baku");

        assertEquals(3L, out.getBookingId());
        verify(bookingCreateService).applyEdit(booking, 88L, null);
    }

    private void stubOwnedCar(Long carId, Long ownerUserId) {
        Customer owner = Customer.builder().userId(ownerUserId).build();
        when(carRepository.findByCarId(carId)).thenReturn(Car.builder().carId(carId).customer(owner).build());
    }
}
