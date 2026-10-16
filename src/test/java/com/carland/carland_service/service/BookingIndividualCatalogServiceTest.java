package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingIndividualServiceFiltersResponse;
import com.carland.carland_service.dto.booking.BookingIndividualServicesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.IndividualServiceFilter;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.CustomerServiceRecord;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.IndividualServiceSource;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerServiceRecordRepository;
import com.carland.carland_service.repository.IndividualServiceFilterRepository;
import com.carland.carland_service.repository.IndividualServiceSourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingIndividualCatalogServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock BranchIndividualServiceRepository branchIndividualServiceRepository;
    @Mock IndividualServiceFilterRepository individualServiceFilterRepository;
    @Mock CarRepository carRepository;
    @Mock IndividualServiceSourceRepository individualServiceSourceRepository;
    @Mock PercentageLifeService percentageLifeService;
    @Mock CustomerServiceRecordRepository customerServiceRecordRepository;

    BookingIndividualCatalogService service;
    ObjectMapper json = JsonMapper.builder().addModule(new Jdk8Module()).build();

    @BeforeEach
    void setUp() {
        service = new BookingIndividualCatalogService(
                branchRepository,
                branchIndividualServiceRepository,
                individualServiceFilterRepository,
                carRepository,
                individualServiceSourceRepository,
                percentageLifeService,
                customerServiceRecordRepository,
                new ObjectMapper());
    }

    @Test
    void listsFiltersById() {
        when(individualServiceFilterRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                IndividualServiceFilter.builder().id(1L).nameAz("Filtrlər").nameEn("Filters").nameRu("Фильтры").build(),
                IndividualServiceFilter.builder().id(2L).nameAz("Əyləclər").nameEn("Brakes").nameRu("Тормоза").build()
        ));

        BookingIndividualServiceFiltersResponse az = service.filters("az");
        assertEquals("Filtrlər", az.getFilters().get(0).getName());
        assertEquals("Əyləclər", az.getFilters().get(1).getName());

        BookingIndividualServiceFiltersResponse en = service.filters("en");
        assertEquals(2, en.getFilters().size());
        assertEquals(1L, en.getFilters().get(0).getId());
        assertEquals("Filters", en.getFilters().get(0).getName());
        assertEquals("Brakes", en.getFilters().get(1).getName());
    }

    @Test
    void returnsOnlyActiveBranchLines() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        Branch branch = Branch.builder().id(12L).active(true).partner(partner).build();
        IndividualService oil = IndividualService.builder()
                .id(3L).code("MYF").titleJson("{\"az\":\"Yağ dəyişimi\",\"en\":\"Oil change\"}")
                .sortOrder(1).active(true).build();
        IndividualService hidden = IndividualService.builder()
                .id(8L).code("SLF").titleJson("{\"az\":\"Salon\"}").sortOrder(2).active(true).build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                BranchIndividualService.builder().branch(branch).individualService(hidden).active(false).build(),
                BranchIndividualService.builder()
                        .branch(branch).individualService(oil).active(true)
                        .priceSimple(35).priceMedium(45).priceComplex(75).build()
        ));

        BookingIndividualServicesResponse out = service.list(12L, "az", "all", null, null, null, null);

        assertEquals(1, out.getServices().size());
        assertEquals("Yağ dəyişimi", out.getServices().get(0).getName());
        assertNull(out.getPreselectedServiceId());
        assertNull(out.getServices().get(0).getPreselected());
        assertNull(out.getServices().get(0).getCatalogServiceId());
        assertEquals(3500, out.getServices().get(0).getPriceMin());
        assertEquals(4500, out.getServices().get(0).getPriceMid());
        assertEquals(7500, out.getServices().get(0).getPriceMax());
        assertNull(out.getServices().get(0).getIndividualServiceMappedPercentage());
    }

    @Test
    void filterIdReturnsOnlyThatFilterInTheSameList() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        Branch branch = Branch.builder().id(12L).active(true).partner(partner).build();
        IndividualServiceFilter fluids = IndividualServiceFilter.builder().id(3L).nameAz("Mayelər").nameEn("Fluids").nameRu("Жидкости").build();
        IndividualServiceFilter filters = IndividualServiceFilter.builder().id(1L).nameAz("Filtrlər").nameEn("Filters").nameRu("Фильтры").build();
        IndividualService oil = IndividualService.builder()
                .id(3L).code("MYF").titleJson("{\"az\":\"Yağ\"}").sortOrder(1).active(true).filter(fluids).build();
        IndividualService fuel = IndividualService.builder()
                .id(8L).code("YNF").titleJson("{\"az\":\"Yanacaq\"}").sortOrder(2).active(true).filter(filters).build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                BranchIndividualService.builder().branch(branch).individualService(fuel).active(true).priceSimple(10).build(),
                BranchIndividualService.builder().branch(branch).individualService(oil).active(true).priceSimple(20).build()
        ));

        BookingIndividualServicesResponse all = service.list(12L, "az", "all", null, null, null, null);
        assertEquals(List.of("MYF", "YNF"), all.getServices().stream().map(s -> s.getCode()).toList());

        BookingIndividualServicesResponse one = service.list(12L, "az", "1", null, null, null, null);
        assertEquals(1, one.getServices().size());
        assertEquals("YNF", one.getServices().get(0).getCode());
        assertNull(one.getServices().get(0).getPriceMid());
    }

    @Test
    void copiesThePercentageScreenAndLeavesAnUnlinkedServiceBlank() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        Branch branch = Branch.builder().id(12L).active(true).partner(partner).build();
        String airCode = IndividualCatalogSync.codeOf("Hava filtri", "Air filter", "Фильтр");
        String sparkCode = IndividualCatalogSync.codeOf("Alışdırma şamları", "Spark plugs", "Свечи");
        IndividualService air = IndividualService.builder()
                .id(19L).code(airCode).titleJson("{\"az\":\"Hava filtri\"}").sortOrder(1).active(true).build();
        IndividualService spark = IndividualService.builder()
                .id(17L).code(sparkCode).titleJson("{\"az\":\"Alışdırma şamları\"}").sortOrder(2).active(true).build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                BranchIndividualService.builder().branch(branch).individualService(spark).active(true).priceSimple(40).build(),
                BranchIndividualService.builder().branch(branch).individualService(air).active(true).priceSimple(15).priceComplex(30).build()
        ));
        Customer owner = Customer.builder().userId(9L).build();
        Car car = Car.builder().carId(5L).customer(owner).mileage(15000L).build();
        when(carRepository.findByCarId(5L)).thenReturn(car);
        when(percentageLifeService.byServiceId(car)).thenReturn(Map.of(
                100L, new PercentageLifeService.Snapshot(30980, 40, 62, 80)
        ));
        when(individualServiceSourceRepository.findByIndividualServiceIdIn(any())).thenReturn(List.of(
                IndividualServiceSource.builder().serviceId(100L).individualServiceId(19L).build()
        ));
        when(customerServiceRecordRepository.findByServiceIdAndCar(100L, car))
                .thenReturn(CustomerServiceRecord.builder().servicedStatus("serviced").build());

        BookingIndividualServicesResponse out = service.list(12L, "en", null, 5L, 9L, null, null);

        assertEquals(2, out.getServices().size());
        assertEquals(airCode, out.getServices().get(0).getCode());
        assertEquals(62, out.getServices().get(0).getIndividualServiceMappedPercentage());
        assertEquals(30980, out.getServices().get(0).getRemainingKm());
        assertEquals(40, out.getServices().get(0).getRemainingMonth());
        assertNull(out.getServices().get(0).getPercentageNote());
        assertEquals(sparkCode, out.getServices().get(1).getCode());
        assertNull(out.getServices().get(1).getIndividualServiceMappedPercentage());
        assertNull(out.getServices().get(1).getRemainingKm());
        assertNull(out.getServices().get(1).getRemainingMonth());
        assertEquals("No information has been entered for this service", out.getServices().get(1).getPercentageNote());
    }

    @Test
    void percentageIsShownButANullServicedStatusStillGetsTheNote() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        Branch branch = Branch.builder().id(12L).active(true).partner(partner).build();
        IndividualService battery = IndividualService.builder()
                .id(4L).code("BAT").titleJson("{\"az\":\"Akkumulyator\"}").sortOrder(1).active(true).build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                BranchIndividualService.builder().branch(branch).individualService(battery).active(true).priceSimple(10).build()
        ));
        Car car = Car.builder().carId(5L).customer(Customer.builder().userId(9L).build()).mileage(1000L).build();
        when(carRepository.findByCarId(5L)).thenReturn(car);
        when(percentageLifeService.byServiceId(car)).thenReturn(Map.of(
                300L, new PercentageLifeService.Snapshot(null, 1071, null, 98)
        ));
        when(individualServiceSourceRepository.findByIndividualServiceIdIn(any())).thenReturn(List.of(
                IndividualServiceSource.builder().serviceId(300L).individualServiceId(4L).build()
        ));

        BookingIndividualServicesResponse out = service.list(12L, "az", null, 5L, 9L, null, null);

        assertEquals(98, out.getServices().get(0).getIndividualServiceMappedPercentage());
        assertNull(out.getServices().get(0).getRemainingKm());
        assertEquals(1071, out.getServices().get(0).getRemainingMonth());
        assertEquals("Məlumat daxil etməmisiniz", out.getServices().get(0).getPercentageNote());
    }

    @Test
    void missingPercentagesLeaveTheFieldNull() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        Branch branch = Branch.builder().id(12L).active(true).partner(partner).build();
        IndividualService oil = IndividualService.builder()
                .id(3L).code("MYF").titleJson("{\"az\":\"Yağ\"}").sortOrder(1).active(true).build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                BranchIndividualService.builder().branch(branch).individualService(oil).active(true).priceSimple(10).build()
        ));
        Car car = Car.builder().carId(5L).customer(Customer.builder().userId(9L).build()).mileage(1000L).build();
        when(carRepository.findByCarId(5L)).thenReturn(car);

        BookingIndividualServicesResponse out = service.list(12L, "az", null, 5L, 9L, null, null);

        assertEquals(1, out.getServices().size());
        assertNull(out.getServices().get(0).getIndividualServiceMappedPercentage());
        assertNull(out.getServices().get(0).getRemainingKm());
        assertNull(out.getServices().get(0).getRemainingMonth());
        assertEquals("Məlumat daxil etməmisiniz", out.getServices().get(0).getPercentageNote());
    }

    @Test
    void carIdAddsRemainingKmAndDaysFromTheSamePercentageRow() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        Branch branch = Branch.builder().id(12L).active(true).partner(partner).build();
        String airCode = IndividualCatalogSync.codeOf("Hava filtri", "Air filter", "Фильтр");
        IndividualService air = IndividualService.builder()
                .id(19L).code(airCode).titleJson("{\"en\":\"Air filter\"}").sortOrder(1).active(true).build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                BranchIndividualService.builder().branch(branch).individualService(air).active(true).priceSimple(15).build()
        ));
        Car car = Car.builder().carId(5L).customer(Customer.builder().userId(9L).build()).mileage(15000L).build();
        when(carRepository.findByCarId(5L)).thenReturn(car);
        when(percentageLifeService.byServiceId(car)).thenReturn(Map.of(
                100L, new PercentageLifeService.Snapshot(5000, 40, 50, 80)
        ));
        when(individualServiceSourceRepository.findByIndividualServiceIdIn(any())).thenReturn(List.of(
                IndividualServiceSource.builder().serviceId(100L).individualServiceId(19L).build()
        ));
        when(customerServiceRecordRepository.findByServiceIdAndCar(100L, car))
                .thenReturn(CustomerServiceRecord.builder().servicedStatus("").build());

        BookingIndividualServicesResponse out = service.list(12L, "en", "all", 5L, 9L, null, null);

        assertEquals(50, out.getServices().get(0).getIndividualServiceMappedPercentage());
        assertEquals(5000, out.getServices().get(0).getRemainingKm());
        assertEquals(40, out.getServices().get(0).getRemainingMonth());
        assertNull(out.getServices().get(0).getPercentageNote());
    }

    @Test
    void rejectsAnotherUsersCar() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(Branch.builder().id(12L).active(true).partner(partner).build()));
        when(carRepository.findByCarId(5L)).thenReturn(
                Car.builder().carId(5L).customer(Customer.builder().userId(1L).build()).build());

        assertThrows(ForbiddenException.class, () -> service.list(12L, "az", null, 5L, 9L, null, null));
    }

    @Test
    void carServiceMarksTheLinkedRowAndKeepsTheFullList() throws Exception {
        Branch branch = openBranch();
        IndividualService oil = serviceRow(3L, "MYF", "Yağ", 1);
        IndividualService fuel = serviceRow(8L, "YNF", "Yanacaq", 2);
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                priced(branch, fuel, 10),
                priced(branch, oil, 20)
        ));
        when(individualServiceSourceRepository.findIndividualServiceIdsByServiceId(143L)).thenReturn(List.of(3L));

        BookingIndividualServicesResponse out = service.list(12L, "en", "all", null, null, " car_service ", 143L);

        assertEquals(List.of("MYF", "YNF"), out.getServices().stream().map(s -> s.getCode()).toList());
        assertEquals(Optional.of(3L), out.getPreselectedServiceId());
        assertEquals(Boolean.TRUE, out.getServices().get(0).getPreselected());
        assertEquals(143L, out.getServices().get(0).getCatalogServiceId());
        assertEquals("Yağ", out.getServices().get(0).getName());
        assertNull(out.getServices().get(1).getPreselected());
        assertNull(out.getServices().get(1).getCatalogServiceId());
        assertNull(out.getServices().get(0).getIndividualServiceMappedPercentage());
        String body = json.writeValueAsString(out);
        assertTrue(body.contains("\"preselectedServiceId\":3"));
        assertTrue(body.contains("\"preselected\":true"));
        assertTrue(body.contains("\"catalogServiceId\":143"));
        assertEquals(1, count(body, "\"preselected\""));
    }

    @Test
    void carServiceWithoutALinkLeavesTheListUnmarked() throws Exception {
        Branch branch = openBranch();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                priced(branch, serviceRow(3L, "MYF", "Yağ", 1), 10)
        ));
        when(individualServiceSourceRepository.findIndividualServiceIdsByServiceId(143L)).thenReturn(List.of());

        BookingIndividualServicesResponse out = service.list(12L, "az", "all", null, null, "CAR_SERVICE", 143L);

        assertEquals(1, out.getServices().size());
        assertEquals(Optional.empty(), out.getPreselectedServiceId());
        assertNull(out.getServices().get(0).getPreselected());
        String body = json.writeValueAsString(out);
        assertTrue(body.contains("\"preselectedServiceId\":null"));
        assertFalse(body.contains("\"preselected\""));
    }

    @Test
    void standardAndUnknownEntryIgnoreCatalogServiceId() throws Exception {
        Branch branch = openBranch();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                priced(branch, serviceRow(3L, "MYF", "Yağ", 1), 10)
        ));

        BookingIndividualServicesResponse standard = service.list(12L, "az", "all", null, null, "STANDARD", 143L);
        BookingIndividualServicesResponse unknown = service.list(12L, "az", "all", null, null, "workshop", 143L);
        BookingIndividualServicesResponse missing = service.list(12L, "az", "all", null, null, "CAR_SERVICE", null);

        assertNull(standard.getPreselectedServiceId());
        assertNull(unknown.getPreselectedServiceId());
        assertEquals(Optional.empty(), missing.getPreselectedServiceId());
        assertFalse(json.writeValueAsString(standard).contains("preselected"));
        verify(individualServiceSourceRepository, never()).findIndividualServiceIdsByServiceId(143L);
    }

    @Test
    void severalLinkedRowsMarkTheSmallestId() {
        Branch branch = openBranch();
        IndividualService oil = serviceRow(3L, "MYF", "Yağ", 1);
        IndividualService fuel = serviceRow(8L, "YNF", "Yanacaq", 2);
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                priced(branch, fuel, 10),
                priced(branch, oil, 20)
        ));
        when(individualServiceSourceRepository.findIndividualServiceIdsByServiceId(143L)).thenReturn(List.of(8L, 3L));

        BookingIndividualServicesResponse out = service.list(12L, "az", "all", null, null, "CAR_SERVICE", 143L);

        assertEquals(2, out.getServices().size());
        assertEquals(Optional.of(3L), out.getPreselectedServiceId());
        assertEquals(Boolean.TRUE, out.getServices().get(0).getPreselected());
        assertNull(out.getServices().get(1).getPreselected());
    }

    @Test
    void filterThatDropsTheLinkedRowDoesNotMarkAnyone() {
        Branch branch = openBranch();
        IndividualServiceFilter fluids = IndividualServiceFilter.builder().id(3L).nameAz("Mayelər").nameEn("Fluids").nameRu("Жидкости").build();
        IndividualServiceFilter filters = IndividualServiceFilter.builder().id(1L).nameAz("Filtrlər").nameEn("Filters").nameRu("Фильтры").build();
        IndividualService oil = serviceRow(3L, "MYF", "Yağ", 1);
        oil.setFilter(fluids);
        IndividualService fuel = serviceRow(8L, "YNF", "Yanacaq", 2);
        fuel.setFilter(filters);
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(branchIndividualServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(
                priced(branch, fuel, 10),
                priced(branch, oil, 20)
        ));
        when(individualServiceSourceRepository.findIndividualServiceIdsByServiceId(143L)).thenReturn(List.of(3L));

        BookingIndividualServicesResponse out = service.list(12L, "az", "1", null, null, "CAR_SERVICE", 143L);

        assertEquals(1, out.getServices().size());
        assertEquals("YNF", out.getServices().get(0).getCode());
        assertEquals(Optional.empty(), out.getPreselectedServiceId());
        assertNull(out.getServices().get(0).getPreselected());
    }

    private static Branch openBranch() {
        return Branch.builder().id(12L).active(true).partner(Partner.builder().id(1L).active(true).build()).build();
    }

    private static IndividualService serviceRow(Long id, String code, String az, int sort) {
        return IndividualService.builder()
                .id(id).code(code).titleJson("{\"az\":\"" + az + "\",\"en\":\"" + az + "\"}")
                .sortOrder(sort).active(true).build();
    }

    private static BranchIndividualService priced(Branch branch, IndividualService catalog, int simple) {
        return BranchIndividualService.builder()
                .branch(branch).individualService(catalog).active(true).priceSimple(simple).build();
    }

    private static int count(String body, String token) {
        int found = 0;
        int from = 0;
        while (true) {
            int at = body.indexOf(token, from);
            if (at < 0) {
                return found;
            }
            found++;
            from = at + token.length();
        }
    }
}
