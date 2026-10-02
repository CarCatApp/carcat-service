package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingQuoteResponse;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.BookingWriteRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingInspection;
import com.carland.carland_service.entity.BookingSelectedService;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchPackage;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BookingSelectedServiceRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchPackageRepository;
import com.carland.carland_service.repository.BranchServiceRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.OfferedServiceRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock BranchCarePackageRepository carePackageRepository;
    @Mock OfferedServiceRepository offeredServiceRepository;
    @Mock BookingSelectedServiceRepository bookingSelectedServiceRepository;
    @Mock CustomerRepository customerRepository;
    @Mock CarRepository carRepository;
    @Mock BookingSelectionWriter selectionWriter;

    BookingCreateService service;
    Range range;
    Branch branch;
    Customer customer;
    Car car;

    @BeforeEach
    void setUp() {
        service = new BookingCreateService(
                rangeRepository, bookingRepository, bookingItemRepository,
                packageRepository, serviceRepository, carePackageRepository, offeredServiceRepository,
                bookingSelectedServiceRepository, customerRepository, carRepository, new ObjectMapper(),
                selectionWriter);
        customer = Customer.builder().userId(77L).phoneNumber("+994501112233").build();
        car = Car.builder().carId(55L).vin("3FA6P0HDXKR168752").customer(customer).build();
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
        stubOwnedCar();
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
        assertEquals(55L, out.getCarId());
        assertEquals("3FA6P0HDXKR168752", out.getVin());
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertEquals(77L, captor.getValue().getCustomerUserId());
        assertEquals(55L, captor.getValue().getCarId());
    }

    @Test
    void createCopiesCarePackageAndOfferedServiceName() {
        stubCatalogAndRange(true);
        stubOwnedCar();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.existsByRef(any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking saved = inv.getArgument(0);
            saved.setId(46L);
            return saved;
        });
        when(carePackageRepository.findById(4L)).thenReturn(Optional.of(BranchCarePackage.builder()
                .id(4L)
                .branch(branch)
                .name("Hyper Xaqani Extra")
                .price(285)
                .active(true)
                .build()));
        when(offeredServiceRepository.findById(12L)).thenReturn(Optional.of(OfferedService.builder()
                .id(12L)
                .titleJson("{\"az\":\"Mühərrik yağı\",\"en\":\"Engine oil\",\"ru\":\"Моторное масло\"}")
                .active(true)
                .build()));
        BookingWriteRequest req = request();
        req.setCarePackageId(4L);
        req.setOfferedServiceIds(List.of(12L));

        BookingView out = service.create(req, 77L, "Asia/Baku");

        assertEquals(12900 + 28500, out.getPriceMin());
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertEquals("Hyper Xaqani Extra", bookingCaptor.getValue().getPackageName());
        assertEquals(28500, bookingCaptor.getValue().getPackagePrice());
        ArgumentCaptor<BookingSelectedService> lineCaptor = ArgumentCaptor.forClass(BookingSelectedService.class);
        verify(bookingSelectedServiceRepository).save(lineCaptor.capture());
        assertEquals(12L, lineCaptor.getValue().getOfferedServiceId());
        assertEquals("{\"az\":\"Mühərrik yağı\",\"en\":\"Engine oil\",\"ru\":\"Моторное масло\"}",
                lineCaptor.getValue().getTitleJson());
    }

    @Test
    void createInstantIsAutoAccepted() {
        range.setBookingMode("instant");
        stubCatalogAndRange(true);
        stubOwnedCar();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.existsByRef(any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(45L);
            return b;
        });

        BookingView out = service.create(request(), 77L, "Asia/Baku");

        assertEquals("auto_accepted", out.getStatus());
        assertEquals("instant", out.getBookingMode());
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertEquals("auto_accepted", captor.getValue().getStatus());
    }

    @Test
    void createStoresPackageServicesAndInspection() {
        stubCatalogAndRange(true);
        stubOwnedCar();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.existsByRef(any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(501L);
            return b;
        });
        when(carePackageRepository.findById(5L)).thenReturn(Optional.of(BranchCarePackage.builder()
                .id(5L).branch(branch).name("Hyper extra").price(129).active(true).build()));
        IndividualService oil = IndividualService.builder()
                .id(3L).code("MYF").titleJson("{\"az\":\"Yağ dəyişimi\"}").active(true).build();
        BranchIndividualService row = BranchIndividualService.builder()
                .individualService(oil).active(true).priceSimple(35).priceComplex(75).build();
        BookingSelectionWriter.Priced priced = new BookingSelectionWriter.Priced(
                List.of(row), "Generator akkumulyatoru doldurmur", 3500, 7500);
        when(selectionWriter.price(eq(branch), eq(List.of(3L)), eq("Generator akkumulyatoru doldurmur")))
                .thenReturn(priced);
        when(selectionWriter.save(any(), any(), any(), eq(priced))).thenReturn(BookingInspection.builder()
                .id(9L)
                .message("Generator akkumulyatoru doldurmur")
                .branchName("Xeqani")
                .build());
        BookingWriteRequest req = BookingWriteRequest.builder()
                .branchId(7L)
                .slotId(105L)
                .carId(55L)
                .carePackageId(5L)
                .individualServiceIds(List.of(3L))
                .issue("Generator akkumulyatoru doldurmur")
                .build();

        BookingView out = service.create(req, 77L, "Asia/Baku", "az");

        assertEquals("Hyper extra", out.getPackageName());
        assertEquals(12900, out.getPackagePrice());
        assertEquals("Yağ dəyişimi", out.getIndividualServices().get(0).getName());
        assertEquals("Generator akkumulyatoru doldurmur", out.getInspection().getMessage());
        assertEquals("Dövri Qulluq + Təmir Xidməti və Yoxlanış", out.getServiceLabel());
        assertEquals(16400, out.getPriceMin());
    }

    @Test
    void createRequiresCarId() {
        stubCatalogAndRange(true);
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        BookingWriteRequest req = request();
        req.setCarId(null);

        assertThrows(MissingFieldException.class, () -> service.create(req, 77L, "Asia/Baku"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createRejectsCarNotOwned() {
        stubCatalogAndRange(true);
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(customerRepository.findByUserId(77L)).thenReturn(customer);
        when(carRepository.findByCarIdAndCustomer(55L, customer)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> service.create(request(), 77L, "Asia/Baku"));
        verify(bookingRepository, never()).save(any());
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

    @Test
    void applyEditSameSlotIgnoresOwnOccupancy() {
        stubCatalogAndRange(true);
        when(bookingRepository.countByRange_RangeIdAndStatusInAndIdNot(eq(105L), any(), eq(44L))).thenReturn(2L);
        Booking booking = Booking.builder()
                .id(44L)
                .branch(branch)
                .range(range)
                .status("pending")
                .build();

        service.applyEdit(booking, 105L, List.of("pkg:hyper-extra"));

        assertEquals(105L, booking.getRange().getRangeId());
        assertEquals(12900, booking.getPriceMin());
        verify(bookingItemRepository).deleteByBooking_Id(44L);
    }

    @Test
    void applyEditFullTargetConflicts() {
        when(rangeRepository.lockByRangeId(105L)).thenReturn(Optional.of(range));
        when(bookingRepository.countByRange_RangeIdAndStatusInAndIdNot(eq(105L), any(), eq(44L))).thenReturn(3L);
        Booking booking = Booking.builder()
                .id(44L)
                .branch(branch)
                .range(range)
                .status("auto_accepted")
                .build();

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.applyEdit(booking, 105L, List.of("pkg:hyper-extra")));
        assertEquals("capacity_full", ex.getMessage());
        verify(bookingItemRepository, never()).deleteByBooking_Id(any());
    }

    private void stubOwnedCar() {
        when(customerRepository.findByUserId(77L)).thenReturn(customer);
        when(carRepository.findByCarIdAndCustomer(55L, customer)).thenReturn(car);
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
                .carId(55L)
                .build();
    }
}
