package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingIndividualServicesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingIndividualCatalogServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock BranchIndividualServiceRepository branchIndividualServiceRepository;

    BookingIndividualCatalogService service;

    @BeforeEach
    void setUp() {
        service = new BookingIndividualCatalogService(
                branchRepository, branchIndividualServiceRepository, new ObjectMapper());
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

        BookingIndividualServicesResponse out = service.list(12L, "az");

        assertEquals(1, out.getServices().size());
        assertEquals("Yağ dəyişimi", out.getServices().get(0).getName());
        assertEquals(3500, out.getServices().get(0).getPriceMin());
        assertEquals(7500, out.getServices().get(0).getPriceMax());
    }
}
