package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingIndividualServiceFiltersResponse;
import com.carland.carland_service.dto.booking.BookingIndividualServicesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.IndividualServiceFilter;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.IndividualServiceFilterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingIndividualCatalogServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock BranchIndividualServiceRepository branchIndividualServiceRepository;
    @Mock IndividualServiceFilterRepository individualServiceFilterRepository;

    BookingIndividualCatalogService service;

    @BeforeEach
    void setUp() {
        service = new BookingIndividualCatalogService(
                branchRepository, branchIndividualServiceRepository, individualServiceFilterRepository, new ObjectMapper());
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

        BookingIndividualServicesResponse out = service.list(12L, "az", "all");

        assertEquals(1, out.getServices().size());
        assertEquals("Yağ dəyişimi", out.getServices().get(0).getName());
        assertEquals(3500, out.getServices().get(0).getPriceMin());
        assertEquals(7500, out.getServices().get(0).getPriceMax());
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

        BookingIndividualServicesResponse all = service.list(12L, "az", "all");
        assertEquals(List.of("MYF", "YNF"), all.getServices().stream().map(s -> s.getCode()).toList());

        BookingIndividualServicesResponse one = service.list(12L, "az", "1");
        assertEquals(1, one.getServices().size());
        assertEquals("YNF", one.getServices().get(0).getCode());
    }
}
