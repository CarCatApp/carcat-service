package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingIndividualServiceFiltersResponse;
import com.carland.carland_service.dto.booking.BookingIndividualServicesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.IndividualServiceFilter;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.IndividualServiceFilterRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingIndividualCatalogServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock BranchIndividualServiceRepository branchIndividualServiceRepository;
    @Mock IndividualServiceFilterRepository individualServiceFilterRepository;
    @Mock CarRepository carRepository;
    @Mock PercentageRepository percentageRepository;
    @Mock ServiceEntityRepository serviceEntityRepository;

    BookingIndividualCatalogService service;

    @BeforeEach
    void setUp() {
        service = new BookingIndividualCatalogService(
                branchRepository,
                branchIndividualServiceRepository,
                individualServiceFilterRepository,
                carRepository,
                percentageRepository,
                serviceEntityRepository,
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
                        .branch(branch).individualService(oil).active(true).priceSimple(35).priceComplex(75).build()
        ));

        BookingIndividualServicesResponse out = service.list(12L, "az", "all", null, null);

        assertEquals(1, out.getServices().size());
        assertEquals("Yağ dəyişimi", out.getServices().get(0).getName());
        assertEquals(3500, out.getServices().get(0).getPriceMin());
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

        BookingIndividualServicesResponse all = service.list(12L, "az", "all", null, null);
        assertEquals(List.of("MYF", "YNF"), all.getServices().stream().map(s -> s.getCode()).toList());

        BookingIndividualServicesResponse one = service.list(12L, "az", "1", null, null);
        assertEquals(1, one.getServices().size());
        assertEquals("YNF", one.getServices().get(0).getCode());
    }

    @Test
    void carIdKeepsOnlyServicesThatCarHasAndPicksTheSoonerPercentage() {
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
        when(carRepository.findByCarId(5L)).thenReturn(Car.builder().carId(5L).customer(owner).mileage(15000L).build());
        ServiceEntity airService = ServiceEntity.builder().id(100L).nameAz("Hava filtri").nameEn("Air filter").nameRu("Фильтр").build();
        when(serviceEntityRepository.findAllById(List.of(100L))).thenReturn(List.of(airService));
        when(percentageRepository.findAllByCarId(5L)).thenReturn(List.of(
                Percentage.builder().id(1L).serviceId(100L).carId(5L)
                        .lastServiceKm(10000).nextServiceKm(20000)
                        .lastServiceDate(LocalDate.of(2026, 1, 1)).nextServiceDate(LocalDate.of(2027, 1, 1))
                        .build(),
                Percentage.builder().id(2L).serviceId(100L).carId(5L)
                        .lastServiceKm(0).nextServiceKm(10000)
                        .lastServiceDate(LocalDate.of(2024, 1, 1)).nextServiceDate(LocalDate.of(2025, 1, 1))
                        .build()
        ));

        BookingIndividualServicesResponse out = service.list(12L, "en", null, 5L, 9L);

        assertEquals(1, out.getServices().size());
        assertEquals(airCode, out.getServices().get(0).getCode());
        assertEquals(0, out.getServices().get(0).getIndividualServiceMappedPercentage());
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
        when(carRepository.findByCarId(5L)).thenReturn(
                Car.builder().carId(5L).customer(Customer.builder().userId(9L).build()).mileage(1000L).build());
        when(percentageRepository.findAllByCarId(5L)).thenReturn(List.of());

        BookingIndividualServicesResponse out = service.list(12L, "az", null, 5L, 9L);

        assertEquals(1, out.getServices().size());
        assertNull(out.getServices().get(0).getIndividualServiceMappedPercentage());
    }

    @Test
    void rejectsAnotherUsersCar() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(Branch.builder().id(12L).active(true).partner(partner).build()));
        when(carRepository.findByCarId(5L)).thenReturn(
                Car.builder().carId(5L).customer(Customer.builder().userId(1L).build()).build());

        assertThrows(ForbiddenException.class, () -> service.list(12L, "az", null, 5L, 9L));
    }
}
