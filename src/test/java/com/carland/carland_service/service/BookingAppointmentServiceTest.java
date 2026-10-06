package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingAppointmentCancelRequest;
import com.carland.carland_service.dto.booking.BookingAppointmentCancelResponse;
import com.carland.carland_service.dto.booking.BookingAppointmentRequest;
import com.carland.carland_service.dto.booking.BookingAppointmentResponse;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.RangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingAppointmentServiceTest {

    @Mock RangeRepository rangeRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BranchCarePackageRepository carePackageRepository;
    @Mock CustomerRepository customerRepository;
    @Mock CarRepository carRepository;
    @Mock BookingCalendarService calendarService;
    @Mock BookingSelectionWriter selectionWriter;
    @Mock BookingCapacityService capacity;

    BookingAppointmentService service;
    Branch branch;
    BranchCarePackage pkg;

    @BeforeEach
    void setUp() {
        service = new BookingAppointmentService(
                rangeRepository, bookingRepository, carePackageRepository, customerRepository,
                carRepository, calendarService, selectionWriter, capacity);
        Partner partner = Partner.builder().id(1L).name("HS").active(true).build();
        branch = Branch.builder().id(7L).name("Babek").active(true).partner(partner).build();
        pkg = BranchCarePackage.builder().id(10L).name("Yağ").active(true).branch(branch).price(40).build();
    }

    @Test
    void instantRangeStoresThePackageOnTheBooking() {
        LocalDate day = LocalDate.now(StaffSlotWindows.ZONE).plusDays(1);
        Range range = range(70L, day, "instant");
        when(rangeRepository.lockByRangeId(70L)).thenReturn(Optional.of(range));
        when(calendarService.selection(any(), any(), any(), any(), any()))
                .thenReturn(new BookingCalendarService.Selection(10L, Set.of(), false));
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(carePackageRepository.findById(10L)).thenReturn(Optional.of(pkg));
        Customer customer = Customer.builder().userId(3L).name("Ali").surname("Veli").phoneNumber("050").build();
        Car car = Car.builder().carId(15L).vin("VIN15").plateNumber("10-AA-010").brand("Toyota").model("Camry").modelYear(2020).customer(customer).build();
        when(customerRepository.findByUserId(3L)).thenReturn(customer);
        when(carRepository.findByCarIdAndCustomer(15L, customer)).thenReturn(car);
        when(bookingRepository.existsByRef(any())).thenReturn(false);
        when(bookingRepository.save(any())).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        BookingAppointmentResponse response = service.create(7L, request(), 3L, "az");

        assertEquals(100L, response.getBookingId());
        assertEquals("auto_accepted", response.getStatus());
        assertEquals("instant", response.getBookingMode());
        assertEquals(70L, response.getRangeId());
        assertEquals(day.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), response.getDate());
        assertEquals("11:00", response.getStart());
        assertEquals("11:30", response.getEnd());
        assertEquals("VIN15", response.getVin());
        assertEquals("10-AA-010", response.getPlateNumber());
        assertEquals(10L, response.getPackageId());
        assertEquals("Yağ", response.getPackageName());
        assertEquals(4000, response.getPackagePrice());
        assertEquals(4000, response.getPriceMin());
        assertEquals("qepik", response.getUnit());
    }

    @Test
    void approvalRangeStaysPending() {
        LocalDate day = LocalDate.now(StaffSlotWindows.ZONE).plusDays(1);
        Range range = range(70L, day, "approval");
        when(rangeRepository.lockByRangeId(70L)).thenReturn(Optional.of(range));
        when(calendarService.selection(any(), any(), any(), any(), any()))
                .thenReturn(new BookingCalendarService.Selection(10L, Set.of(), false));
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(carePackageRepository.findById(10L)).thenReturn(Optional.of(pkg));
        Customer customer = Customer.builder().userId(3L).build();
        Car car = Car.builder().carId(15L).vin("VIN15").customer(customer).build();
        when(customerRepository.findByUserId(3L)).thenReturn(customer);
        when(carRepository.findByCarIdAndCustomer(15L, customer)).thenReturn(car);
        when(bookingRepository.existsByRef(any())).thenReturn(false);
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BookingAppointmentResponse response = service.create(7L, request(), 3L, "az");

        assertEquals("pending", response.getStatus());
        assertEquals("approval", response.getBookingMode());
    }

    @Test
    void fullRangeIsRejected() {
        LocalDate day = LocalDate.now(StaffSlotWindows.ZONE).plusDays(1);
        Range range = range(70L, day, "instant");
        when(rangeRepository.lockByRangeId(70L)).thenReturn(Optional.of(range));
        when(calendarService.selection(any(), any(), any(), any(), any()))
                .thenReturn(new BookingCalendarService.Selection(10L, Set.of(), false));
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(2L);

        assertThrows(ConflictException.class, () -> service.create(7L, request(), 3L, "az"));
        verify(bookingRepository, never()).save(any());
    }

    private BookingAppointmentRequest request() {
        BookingAppointmentRequest body = new BookingAppointmentRequest();
        body.setRangeId(70L);
        body.setPackageId(10L);
        body.setCarId(15L);
        return body;
    }

    @Test
    void cancelWritesTheReasonWhenTheBookingBelongsToTheUser() {
        Booking booking = Booking.builder()
                .id(12L)
                .ref("CC-091214")
                .customerUserId(54L)
                .status("auto_accepted")
                .build();
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));

        BookingAppointmentCancelResponse out = service.cancel(
                12L, new BookingAppointmentCancelRequest("plan dəyişdi"), 54L, "az");

        assertEquals("cancelled", out.getStatus());
        assertEquals("plan dəyişdi", out.getReason());
        assertEquals("cancelled", booking.getStatus());
        assertEquals("plan dəyişdi", booking.getCancelNote());
        assertEquals(BookingAppointmentService.CUSTOMER_REASON, booking.getCancelReasonCode());
    }

    @Test
    void cancelRefusesAnotherUsersBooking() {
        Booking booking = Booking.builder().id(12L).customerUserId(54L).status("pending").build();
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class, () -> service.cancel(
                12L, new BookingAppointmentCancelRequest("plan dəyişdi"), 9L, "az"));
        assertEquals("pending", booking.getStatus());
    }

    @Test
    void cancelRefusesACompletedBooking() {
        Booking booking = Booking.builder().id(12L).customerUserId(54L).status("completed").build();
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));

        assertThrows(ConflictException.class, () -> service.cancel(
                12L, new BookingAppointmentCancelRequest("plan dəyişdi"), 54L, "az"));
        assertEquals("completed", booking.getStatus());
    }

    private Range range(Long id, LocalDate day, String mode) {
        OffsetDateTime start = ZonedDateTime.of(day, LocalTime.of(11, 0), StaffSlotWindows.ZONE).toOffsetDateTime();
        return Range.builder()
                .rangeId(id)
                .start(start)
                .end(start.plusMinutes(30))
                .workerCount(2)
                .status(RangeStatus.AVAILABLE.name())
                .bookingMode(mode)
                .slotTarget(StaffSlotTargets.PACKAGE)
                .carePackage(pkg)
                .calendar(Calendar.builder().day(day).branch(branch).serviceCategory(StaffSlotTargets.CALENDAR_CATEGORY).build())
                .build();
    }
}
