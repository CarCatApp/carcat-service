package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.StaffCompleteRequest;
import com.carland.carland_service.dto.request.StaffCompleteRequest.StaffCompleteLine;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchCarePackageItem;
import com.carland.carland_service.entity.BrandModel;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingIndividualLineRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BookingInspectionRepository;
import com.carland.carland_service.repository.BookingSelectedServiceRepository;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BrandModelRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import com.carland.carland_service.hyper.CarByVin;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.OfferedServiceRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.service.webhook.VisitWebhookSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCompletionServiceTest {

    @Mock BranchCarePackageItemRepository carePackageItemRepository;
    @Mock BranchCarePackageRepository carePackageRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingIndividualLineRepository individualLineRepository;
    @Mock BookingSelectedServiceRepository selectedServiceRepository;
    @Mock BookingInspectionRepository inspectionRepository;
    @Mock BranchIndividualServiceRepository branchIndividualServiceRepository;
    @Mock IndividualServiceRepository individualServiceRepository;
    @Mock OfferedServiceRepository offeredServiceRepository;
    @Mock BrandModelRepository brandModelRepository;
    @Mock BrandModelServiceRepository brandModelServiceRepository;
    @Mock CarRepository carRepository;
    @Mock CarByVin carByVin;
    @Mock VisitRepository visitRepository;
    @Mock ServiceEntityRepository serviceEntityRepository;
    @Mock ServiceCategoryJson json;
    @Mock BookingPercentageSnapshot bookingPercentageSnapshot;
    @Mock VisitWebhookSupport visitWebhookSupport;
    @Mock DetailedPercentageService detailedPercentageService;
    @Mock RedisCacheService redisCacheService;
    @Mock BookingPaidAmountService bookingPaidAmountService;

    BookingCompletionService service;
    Booking booking;
    Car car;
    OfferedService oil;

    @BeforeEach
    void setUp() {
        service = new BookingCompletionService(carePackageItemRepository, individualLineRepository,
                selectedServiceRepository, inspectionRepository, branchIndividualServiceRepository,
                individualServiceRepository, offeredServiceRepository, brandModelRepository,
                brandModelServiceRepository, carRepository, carByVin, visitRepository, serviceEntityRepository, json,
                bookingPercentageSnapshot, visitWebhookSupport, detailedPercentageService, redisCacheService,
                bookingRepository, carePackageRepository, bookingPaidAmountService);
        Partner partner = Partner.builder().id(1L).name("HyperService").active(true).build();
        Branch branch = Branch.builder().id(7L).name("Xeqani").active(true).partner(partner).build();
        booking = Booking.builder().id(50L).ref("CC-042197").branch(branch).carId(5L)
                .carePackageId(3L).packageName("Hyper extra").packagePrice(12900).build();
        car = Car.builder().carId(5L).mileage(40000L).build();
        oil = OfferedService.builder().id(1L).titleJson("{}").periodic(true).active(true).maintenanceCode("ENGINE_OIL").build();
        OfferedService scan = OfferedService.builder().id(2L).titleJson("{}").periodic(false).active(true).build();

        lenient().when(json.read(any())).thenReturn(Map.of("az", "Xidmət"));
        when(carRepository.findByCarId(5L)).thenReturn(car);
        lenient().when(carePackageItemRepository.findByCarePackage_IdOrderByIdAsc(3L)).thenReturn(List.of(
                BranchCarePackageItem.builder().id(31L).offeredService(oil).build(),
                BranchCarePackageItem.builder().id(32L).offeredService(scan).build()));
        lenient().when(inspectionRepository.findByBooking_Id(50L)).thenReturn(Optional.empty());
        lenient().when(brandModelRepository.findByBranch_IdOrderByIdAsc(7L)).thenReturn(List.of(
                BrandModel.builder().id(11L).name("Castrol EDGE").price(new BigDecimal("25")).build(),
                BrandModel.builder().id(12L).name("Brembo").price(new BigDecimal("15")).build()));
        lenient().when(visitRepository.saveAndFlush(any(Visit.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car)).thenReturn(List.of());
    }

    @Test
    void writesPackageLineDiscountsAndOverallDiscount() {
        when(offeredServiceRepository.findById(1L)).thenReturn(Optional.of(oil));
        when(individualServiceRepository.findById(20L)).thenReturn(Optional.of(
                IndividualService.builder().id(20L).code("BRK").titleJson("{}").maintenanceCode("BRAKE_PADS").build()));

        StaffCompleteRequest request = StaffCompleteRequest.builder()
                .date(LocalDate.of(2026, 10, 27))
                .mileage(42580)
                .discountPercent(new BigDecimal("10"))
                .notes("ümumi qeyd")
                .lines(List.of(
                        // Paket: qiymət daxildir, göndərilən qiymət nəzərə alınmır.
                        StaffCompleteLine.builder().kind("package").offeredServiceId(1L)
                                .productName("Castrol").productUnit("litr")
                                .productQty(new BigDecimal("4")).productAmount(BigDecimal.ZERO)
                                .price(new BigDecimal("999"))
                                .nextMileage(52580).build(),
                        // typed total 30 −10% → 27; service 50 −10% → 45; line final 72
                        StaffCompleteLine.builder().kind("individual").individualServiceId(20L)
                                .productName("Brembo")
                                .productUnit("ədəd")
                                .productQty(new BigDecimal("2"))
                                .productAmount(new BigDecimal("30"))
                                .productDiscountPercent(new BigDecimal("10"))
                                .price(new BigDecimal("50"))
                                .discountPercent(new BigDecimal("10")).build(),
                        // service 100 → manual 90, discount back-computed as 10%. Product total is typed as 0.
                        StaffCompleteLine.builder().kind("one_time").title("Dəmir qapının dəyişdirilməsi")
                                .productName("Boya").productUnit("ədəd").productQty(BigDecimal.ONE)
                                .productAmount(BigDecimal.ZERO)
                                .price(new BigDecimal("100")).finalAmount(new BigDecimal("90"))
                                .nextDate(LocalDate.of(2027, 1, 1)).build()))
                .build();

        Visit visit = service.write(booking, request, "Asia/Baku", "az");

        // gross 129 + 0 + 80 + 100 = 309, net 129 + 0 + 72 + 90 = 291, total 291 − 10% = 261.90
        assertEquals(new BigDecimal("309.00"), visit.getCostAmount());
        assertEquals(new BigDecimal("261.90"), visit.getFinalCostAmount());
        assertEquals(new BigDecimal("129.00"), visit.getPackagePrice());
        assertEquals("ümumi qeyd", visit.getNotes());
        assertEquals("panel", visit.getSource());
        assertNull(visit.getHyperRecordId());
        assertEquals(7L, visit.getBranchId());
        assertEquals(1L, visit.getServiceCenterId());
        assertEquals("HyperService", visit.getServiceCenterName());
        assertEquals(3, visit.getServices().size());

        VisitServiceLine pkg = visit.getServices().get(0);
        assertEquals("package", pkg.getKind());
        assertEquals("ENGINE_OIL", pkg.getUniversalServiceId());
        assertEquals(new BigDecimal("0.00"), pkg.getCostAmount());
        assertNull(pkg.getProductAmount());
        assertEquals(52580, pkg.getNextServiceMileage());

        VisitServiceLine individual = visit.getServices().get(1);
        assertEquals(new BigDecimal("30.00"), individual.getProductAmount());
        assertEquals(new BigDecimal("10.00"), individual.getProductDiscountPercent());
        assertEquals(new BigDecimal("72.00"), individual.getFinalAmount());

        VisitServiceLine oneTime = visit.getServices().get(2);
        assertEquals(new BigDecimal("10.00"), oneTime.getDiscountPercent());
        assertNull(oneTime.getNextServiceDate(), "AC9: one-off lines have no next date");
        assertNull(oneTime.getUniversalServiceId());

        assertEquals(3, visit.getParts().size());
        assertNull(visit.getParts().get(0).getPrice());
        assertEquals("litr", visit.getParts().get(0).getUnit());
        assertEquals(new BigDecimal("27.00"), visit.getParts().get(1).getPrice());
        assertEquals("ədəd", visit.getParts().get(1).getUnit());
        assertEquals(42580L, car.getMileage());
        verify(detailedPercentageService).applyFromVisit(car, booking, visit);
    }

    @Test
    void oneTimeLineWithoutTitleIsRejected() {
        StaffCompleteRequest request = StaffCompleteRequest.builder()
                .mileage(42580)
                .lines(List.of(StaffCompleteLine.builder().kind("one_time").title("  ").price(BigDecimal.TEN).build()))
                .build();
        assertThrows(MissingFieldException.class, () -> service.write(booking, request, "Asia/Baku", "az"));
    }

    @Test
    void negativeAmountsAndFinalAboveAmountAreRejected() {
        StaffCompleteRequest negative = StaffCompleteRequest.builder()
                .mileage(42580)
                .lines(List.of(StaffCompleteLine.builder().kind("one_time").title("X").price(new BigDecimal("-1")).build()))
                .build();
        assertThrows(MissingFieldException.class, () -> service.write(booking, negative, "Asia/Baku", "az"));

        StaffCompleteRequest above = StaffCompleteRequest.builder()
                .mileage(42580)
                .lines(List.of(StaffCompleteLine.builder().kind("one_time").title("X")
                        .productName("Boya").productUnit("ədəd").productQty(BigDecimal.ONE)
                        .productAmount(BigDecimal.ONE)
                        .price(BigDecimal.TEN).finalAmount(new BigDecimal("11")).build()))
                .build();
        assertThrows(ConflictException.class, () -> service.write(booking, above, "Asia/Baku", "az"));
    }

    @Test
    void removedPackageDropsTheBookingPriceAndTheVisit() {
        booking.setPriceMin(20000);
        booking.setPriceMax(30000);
        Visit visit = service.write(booking, StaffCompleteRequest.builder()
                .mileage(42580)
                .syncPackage(true)
                .build(), "Asia/Baku", "az");

        assertNull(visit.getPackageName());
        assertNull(visit.getPackagePrice());
        assertEquals(new BigDecimal("0.00"), visit.getFinalCostAmount());
        assertEquals(0, visit.getServices().size());
        assertNull(booking.getCarePackageId());
        assertNull(booking.getPackageName());
        assertNull(booking.getPackagePrice());
        assertEquals(7100, booking.getPriceMin());
        assertEquals(17100, booking.getPriceMax());
        verify(bookingRepository).save(booking);
    }

    @Test
    void replacedPackageTakesTheCatalogPrice() {
        booking.setPriceMin(20000);
        booking.setPriceMax(30000);
        BranchCarePackage newer = BranchCarePackage.builder()
                .id(9L).name("Standart").price(50).active(true).branch(booking.getBranch()).build();
        when(carePackageRepository.findById(9L)).thenReturn(Optional.of(newer));
        when(carePackageItemRepository.findByCarePackage_IdOrderByIdAsc(9L)).thenReturn(List.of());

        Visit visit = service.write(booking, StaffCompleteRequest.builder()
                .mileage(42580)
                .syncPackage(true)
                .carePackageId(9L)
                .build(), "Asia/Baku", "az");

        assertEquals("Standart", visit.getPackageName());
        assertEquals(new BigDecimal("50.00"), visit.getPackagePrice());
        assertEquals(new BigDecimal("50.00"), visit.getFinalCostAmount());
        assertEquals(9L, booking.getCarePackageId());
        assertEquals("Standart", booking.getPackageName());
        assertEquals(5000, booking.getPackagePrice());
        assertEquals(12100, booking.getPriceMin());
        assertEquals(22100, booking.getPriceMax());
    }

    @Test
    void samePackageKeepsTheBookedPrice() {
        Visit visit = service.write(booking, StaffCompleteRequest.builder()
                .mileage(42580)
                .syncPackage(true)
                .carePackageId(3L)
                .build(), "Asia/Baku", "az");

        assertEquals(new BigDecimal("129.00"), visit.getPackagePrice());
        assertEquals("Hyper extra", visit.getPackageName());
        assertEquals(12900, booking.getPackagePrice());
        assertEquals(3L, booking.getCarePackageId());
    }

    @Test
    void packageFromAnotherBranchIsRejected() {
        Branch other = Branch.builder().id(8L).name("Digər").active(true).build();
        when(carePackageRepository.findById(9L)).thenReturn(Optional.of(
                BranchCarePackage.builder().id(9L).name("Başqa").price(10).active(true).branch(other).build()));
        StaffCompleteRequest request = StaffCompleteRequest.builder()
                .mileage(42580)
                .syncPackage(true)
                .carePackageId(9L)
                .build();
        assertThrows(ResourceNotFoundException.class, () -> service.write(booking, request, "Asia/Baku", "az"));
    }

    @Test
    void nonPackageLineRequiresAManualProduct() {
        StaffCompleteRequest request = StaffCompleteRequest.builder()
                .mileage(42580)
                .lines(List.of(StaffCompleteLine.builder().kind("one_time").title("X")
                        .price(BigDecimal.TEN).build()))
                .build();
        assertThrows(MissingFieldException.class, () -> service.write(booking, request, "Asia/Baku", "az"));
    }

    @Test
    void emptyFormStillWritesVisit() {
        Visit visit = service.write(booking, StaffCompleteRequest.builder().mileage(42580).build(), "Asia/Baku", "az");
        assertEquals(new BigDecimal("129.00"), visit.getFinalCostAmount());
        assertEquals(0, visit.getServices().size());
    }
}
