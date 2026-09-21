package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCatalogResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchPackage;
import com.carland.carland_service.entity.BranchService;
import com.carland.carland_service.entity.BranchServiceBrand;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchPackageRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServiceBrandRepository;
import com.carland.carland_service.repository.BranchServiceRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCatalogServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock BranchPackageRepository packageRepository;
    @Mock BranchServiceRepository serviceRepository;
    @Mock BranchServiceBrandRepository brandRepository;

    BookingCatalogService service;
    Partner hyper;
    Branch branch;

    @BeforeEach
    void setUp() {
        service = new BookingCatalogService(
                branchRepository, packageRepository, serviceRepository, brandRepository, new ObjectMapper());
        hyper = Partner.builder().id(1L).name("Hyper").active(true).source("hyper").build();
        branch = Branch.builder().id(7L).name("Xeqani").active(true).partner(hyper).build();
    }

    @Test
    void returnsSeededPackageAndOil() {
        BranchService oil = BranchService.builder()
                .id(21L)
                .branch(branch)
                .serviceKey("svc:oil-change")
                .kind("SERVICE")
                .titleJson("{\"az\":\"Yağ dəyişimi\",\"en\":\"Oil change\",\"ru\":\"Замена масла\"}")
                .priceMin(3500)
                .priceMax(7500)
                .currency("AZN")
                .durationMin(30)
                .active(true)
                .build();
        BranchService repair = BranchService.builder()
                .id(22L)
                .branch(branch)
                .serviceKey("dir:repair")
                .kind("DIRECTION")
                .titleJson("{\"az\":\"Təmir\",\"en\":\"Repair\",\"ru\":\"Ремонт\"}")
                .active(true)
                .build();
        BranchPackage extra = BranchPackage.builder()
                .id(5L)
                .branch(branch)
                .serviceKey("pkg:hyper-extra")
                .titleJson("{\"az\":\"Hyper Extra\",\"en\":\"Hyper Extra\",\"ru\":\"Hyper Extra\"}")
                .priceMin(12900)
                .priceMax(12900)
                .currency("AZN")
                .durationMin(120)
                .includedServiceKeys("[\"svc:oil-change\",\"svc:air-filter\",\"svc:cabin-filter\"]")
                .active(true)
                .build();
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        when(serviceRepository.findByBranchIdAndActiveTrueOrderByIdAsc(7L)).thenReturn(List.of(oil, repair));
        when(packageRepository.findByBranchIdAndActiveTrueOrderByIdAsc(7L)).thenReturn(List.of(extra));
        when(brandRepository.findByBranchService_IdIn(any())).thenReturn(List.of(
                BranchServiceBrand.builder().id(1L).branchService(oil).brand("Castrol").build(),
                BranchServiceBrand.builder().id(2L).branchService(oil).brand("Mobil").build()
        ));

        BookingCatalogResponse out = service.catalog(7L);

        assertEquals(7L, out.getBranchId());
        assertEquals("pkg:hyper-extra", out.getPackages().get(0).getServiceKey());
        assertEquals(12900, out.getPackages().get(0).getPriceMin());
        assertEquals("qepik", out.getPackages().get(0).getUnit());
        assertEquals(List.of("svc:oil-change", "svc:air-filter", "svc:cabin-filter"),
                out.getPackages().get(0).getIncludedServiceKeys());
        assertEquals("svc:oil-change", out.getServices().get(0).getServiceKey());
        assertEquals(List.of("Castrol", "Mobil"), out.getServices().get(0).getBrands());
        assertEquals("dir:repair", out.getDirections().get(0).getServiceKey());
        assertEquals("Repair", out.getDirections().get(0).getTitle().get("en"));
    }

    @Test
    void missingBranchIsNotFound() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.catalog(99L));
    }

    @Test
    void inactiveBranchIsNotFound() {
        branch.setActive(false);
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        assertThrows(ResourceNotFoundException.class, () -> service.catalog(7L));
    }
}
