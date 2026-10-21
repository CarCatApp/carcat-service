package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.StaffCompleteRequest;
import com.carland.carland_service.dto.request.StaffCompleteRequest.StaffCompleteLine;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackageItem;
import com.carland.carland_service.entity.BranchServicedGood;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.BookingIndividualLineRepository;
import com.carland.carland_service.repository.BookingInspectionRepository;
import com.carland.carland_service.repository.BookingSelectedServiceRepository;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchServicedGoodRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.OfferedServiceRepository;
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
    @Mock BookingIndividualLineRepository individualLineRepository;
    @Mock BookingSelectedServiceRepository selectedServiceRepository;
    @Mock BookingInspectionRepository inspectionRepository;
    @Mock BranchIndividualServiceRepository branchIndividualServiceRepository;
    @Mock IndividualServiceRepository individualServiceRepository;
    @Mock OfferedServiceRepository offeredServiceRepository;
    @Mock BranchServicedGoodRepository servicedGoodRepository;
    @Mock CarRepository carRepository;
    @Mock VisitRepository visitRepository;
    @Mock ServiceCategoryJson json;
    @Mock BookingBranchBrandsService bookingBranchBrandsService;
    @Mock VisitWebhookSupport visitWebhookSupport;
    @Mock DetailedPercentageService detailedPercentageService;
    @Mock RedisCacheService redisCacheService;

    BookingCompletionService service;
    Booking booking;
    Car car;
    OfferedService oil;

    @BeforeEach
    void setUp() {
        service = new BookingCompletionService(carePackageItemRepository, individualLineRepository,
                selectedServiceRepository, inspectionRepository, branchIndividualServiceRepository,
                individualServiceRepository, offeredServiceRepository, servicedGoodRepository, carRepository,
                visitRepository, json, bookingBranchBrandsService, visitWebhookSupport, detailedPercentageService,
                redisCacheService);
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
        lenient().when(servicedGoodRepository.findByBranch_IdOrderByIdAsc(7L)).thenReturn(List.of(
                BranchServicedGood.builder().id(11L).name("Castrol EDGE").price(new BigDecimal("25")).build(),
                BranchServicedGood.builder().id(12L).name("Brembo").price(new BigDecimal("15")).build()));
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
                .lines(List.of(
                        // Paket: qiymət daxildir, göndərilən qiymət nəzərə alınmır.
                        StaffCompleteLine.builder().kind("package").offeredServiceId(1L).goodId(11L)
                                .productQty(new BigDecimal("4")).price(new BigDecimal("999"))
                                .nextMileage(52580).build(),
                        // 50 + 15×2 = 80, 10% → 72
                        StaffCompleteLine.builder().kind("individual").individualServiceId(20L).goodId(12L)
                                .productQty(new BigDecimal("2")).price(new BigDecimal("50"))
                                .discountPercent(new BigDecimal("10")).build(),
                        // 100 → manual 90, discount back-computed as 10%
                        StaffCompleteLine.builder().kind("one_time").title("Dəmir qapının dəyişdirilməsi")
                                .price(new BigDecimal("100")).finalAmount(new BigDecimal("90"))
                                .nextDate(LocalDate.of(2027, 1, 1)).build()))
                .build();

        Visit visit = service.write(booking, request, "Asia/Baku", "az");

        // gross 129 + 0 + 80 + 100 = 309, net 129 + 0 + 72 + 90 = 291, total 291 − 10% = 261.90
        assertEquals(new BigDecimal("309.00"), visit.getCostAmount());
        assertEquals(new BigDecimal("261.90"), visit.getFinalCostAmount());
        assertEquals(new BigDecimal("129.00"), visit.getPackagePrice());
        assertEquals("panel", visit.getSource());
        assertNull(visit.getHyperRecordId());
        assertEquals(7L, visit.getBranchId());
        assertEquals(1L, visit.getServiceCenterId());
        assertEquals("HyperService", visit.getServiceCenterName());
        assertEquals(4, visit.getServices().size(), "3 cards + 1 package info line");

        VisitServiceLine pkg = visit.getServices().get(0);
        assertEquals("package", pkg.getKind());
        assertEquals("ENGINE_OIL", pkg.getUniversalServiceId());
        assertEquals(new BigDecimal("0.00"), pkg.getCostAmount());
        assertNull(pkg.getProductAmount());
        assertEquals(52580, pkg.getNextServiceMileage());

        VisitServiceLine individual = visit.getServices().get(1);
        assertEquals(new BigDecimal("30.00"), individual.getProductAmount());
        assertEquals(new BigDecimal("72.00"), individual.getFinalAmount());

        VisitServiceLine oneTime = visit.getServices().get(2);
        assertEquals(new BigDecimal("10.00"), oneTime.getDiscountPercent());
        assertNull(oneTime.getNextServiceDate(), "AC9: one-off lines have no next date");
        assertNull(oneTime.getUniversalServiceId());

        assertEquals("package_info", visit.getServices().get(3).getKind());
        assertEquals(2, visit.getParts().size());
        assertNull(visit.getParts().get(0).getPrice(), "package product is included in the package price");
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
                        .price(BigDecimal.TEN).finalAmount(new BigDecimal("11")).build()))
                .build();
        assertThrows(ConflictException.class, () -> service.write(booking, above, "Asia/Baku", "az"));
    }

    @Test
    void emptyFormStillWritesVisit() {
        // AC18: operator təsdiq etdisə, xidmətsiz ziyarət də yazılır (yalnız paket + info sətri).
        Visit visit = service.write(booking, StaffCompleteRequest.builder().mileage(42580).build(), "Asia/Baku", "az");
        assertEquals(new BigDecimal("129.00"), visit.getFinalCostAmount());
        assertEquals(1, visit.getServices().size());
    }
}
