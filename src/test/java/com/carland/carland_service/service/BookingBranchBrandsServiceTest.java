package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBrandModelsResponse;
import com.carland.carland_service.entity.BrandModel;
import com.carland.carland_service.entity.BrandModelService;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BrandModelRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingBranchBrandsServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock BrandModelServiceRepository brandModelServiceRepository;
    @Mock BrandModelRepository brandModelRepository;

    BookingBranchBrandsService service;
    Partner partner;
    Branch branch;

    @BeforeEach
    void setUp() {
        service = new BookingBranchBrandsService(
                branchRepository,
                brandModelServiceRepository,
                brandModelRepository,
                new ServiceCategoryJson(new ObjectMapper()));
        partner = Partner.builder().id(1L).name("Hyper").active(true).build();
        branch = Branch.builder().id(12L).name("Xeqani").active(true).partner(partner).build();
    }

    @Test
    void returnsChipsAndSkipsEmptyHeading() {
        BrandModelService air = BrandModelService.builder()
                .id(1L).titleJson("{\"az\":\"Filtrlər\",\"en\":\"Filters\",\"ru\":\"Фильтры\"}").oil(false).sortOrder(2).build();
        BrandModelService empty = BrandModelService.builder()
                .id(2L).titleJson("{\"az\":\"Təkərlər\",\"en\":\"Tires\",\"ru\":\"Шины\"}").oil(false).sortOrder(7).build();
        BrandModelService oil = BrandModelService.builder()
                .id(4L).titleJson("{\"az\":\"Yağlar\",\"en\":\"Oils\",\"ru\":\"Масла\"}").oil(true).sortOrder(1).build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(brandModelServiceRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(oil, air, empty));
        when(brandModelRepository.findByBranch_IdOrderByIdAsc(12L)).thenReturn(List.of(
                BrandModel.builder().id(11L).brandModelService(air).branch(branch).name("Bosch").unit("eded").build(),
                BrandModel.builder().id(10L).brandModelService(air).branch(branch).name("Mann-Filter").unit("eded").build(),
                BrandModel.builder().id(40L).brandModelService(oil).branch(branch).name("Castrol Edge").series("5W-30").viscosity("15W-30").unit("litr").build()
        ));

        BookingBrandModelsResponse out = service.list(12L, "en");

        assertEquals(12L, out.getBranchId());
        assertEquals(2, out.getGroups().size());
        assertEquals("Oils", out.getGroups().get(0).getTitle());
        assertEquals("Castrol Edge", out.getGroups().get(0).getBrands().get(0).getName());
        assertEquals(40L, out.getGroups().get(0).getBrands().get(0).getId());
        assertEquals("Filters", out.getGroups().get(1).getTitle());
        assertEquals(List.of(11L, 10L), out.getGroups().get(1).getBrands().stream().map(b -> b.getId()).toList());
        assertEquals("Bosch", out.getGroups().get(1).getBrands().get(0).getName());
    }

    @Test
    void emptyBranchReturnsNoGroups() {
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(brandModelServiceRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                BrandModelService.builder().id(1L).titleJson("{\"az\":\"Yağlar\"}").oil(true).sortOrder(1).build()
        ));
        when(brandModelRepository.findByBranch_IdOrderByIdAsc(12L)).thenReturn(List.of());

        BookingBrandModelsResponse out = service.list(12L, "az");

        assertEquals(12L, out.getBranchId());
        assertTrue(out.getGroups().isEmpty());
    }

    @Test
    void missingBranchIsNotFound() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.list(99L, "az"));
    }

    @Test
    void inactiveBranchIsNotFound() {
        branch.setActive(false);
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        assertThrows(ResourceNotFoundException.class, () -> service.list(12L, "az"));
    }
}
