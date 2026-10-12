package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBranchListResponse;
import com.carland.carland_service.dto.booking.BookingDiscoveryPartnerView;
import com.carland.carland_service.dto.booking.BookingDiscoveryResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.IndividualServiceSourceRepository;
import com.carland.carland_service.repository.RangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingDiscoveryServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock IndividualServiceSourceRepository individualServiceSourceRepository;
    @Mock BranchIndividualServiceRepository branchIndividualServiceRepository;
    @Mock RangeRepository rangeRepository;
    @InjectMocks BookingDiscoveryService service;

    Partner hyper;
    Branch baku;
    Branch far;

    @BeforeEach
    void setUp() {
        hyper = Partner.builder().id(1L).name("Hyper").source("hyper").active(true).build();
        baku = Branch.builder()
                .id(7L)
                .name("Babek")
                .address("Babek pr.")
                .lat(40.379)
                .lng(49.846)
                .active(true)
                .partner(hyper)
                .build();
        far = Branch.builder()
                .id(8L)
                .name("Ganja")
                .lat(40.682)
                .lng(46.360)
                .active(true)
                .partner(hyper)
                .build();
    }

    @Test
    void returnsActiveHyperBranch() {
        when(branchRepository.findActiveWithActivePartner()).thenReturn(List.of(baku));

        BookingDiscoveryResponse out = service.discover(null, null, null, null, null, 1, 20);

        assertEquals(1, out.getPage());
        assertEquals(1, out.getPartners().size());
        BookingDiscoveryPartnerView partner = out.getPartners().get(0);
        assertEquals(1L, partner.getPartnerId());
        assertEquals("Hyper", partner.getName());
        assertEquals(7L, partner.getBranches().get(0).getBranchId());
    }

    @Test
    void listActiveReturnsEveryActiveBranch() {
        Branch closedPartnerBranch = Branch.builder().id(9L).name("Closed").active(true).partner(hyper).build();
        when(branchRepository.findActiveWithActivePartner()).thenReturn(List.of(baku, far));

        BookingBranchListResponse out = service.listActive(null);

        assertEquals(2, out.getBranches().size());
        assertEquals(7L, out.getBranches().get(0).getBranchId());
        assertEquals("Hyper", out.getBranches().get(0).getPartnerName());
        assertEquals(8L, out.getBranches().get(1).getBranchId());
        assertTrue(out.getBranches().stream().noneMatch(b -> b.getBranchId().equals(closedPartnerBranch.getId())));
    }

    @Test
    void searchByBranchName() {
        when(branchRepository.findActiveWithActivePartner()).thenReturn(List.of(baku, far));

        BookingDiscoveryResponse out = service.discover(null, "ganja", null, null, null, 1, 20);

        assertEquals(1, out.getPartners().size());
        assertEquals(8L, out.getPartners().get(0).getBranches().get(0).getBranchId());
    }

    @Test
    void geoKeepsNearbyDropsFar() {
        when(branchRepository.findActiveWithActivePartner()).thenReturn(List.of(baku, far));

        BookingDiscoveryResponse out = service.discover(null, null, 40.379, 49.846, 5000, 1, 20);

        assertEquals(1, out.getPartners().size());
        assertEquals(1, out.getPartners().get(0).getBranches().size());
        assertEquals(7L, out.getPartners().get(0).getBranches().get(0).getBranchId());
    }

    @Test
    void geoDropsBranchWithoutCoords() {
        Branch noGeo = Branch.builder().id(9L).name("NoPin").active(true).partner(hyper).build();
        when(branchRepository.findActiveWithActivePartner()).thenReturn(List.of(noGeo));

        BookingDiscoveryResponse out = service.discover(null, null, 40.379, 49.846, 5000, 1, 20);

        assertTrue(out.getPartners().isEmpty());
    }

    @Test
    void paginationSecondPageEmpty() {
        when(branchRepository.findActiveWithActivePartner()).thenReturn(List.of(baku));

        BookingDiscoveryResponse out = service.discover(null, null, null, null, null, 2, 20);

        assertEquals(2, out.getPage());
        assertTrue(out.getPartners().isEmpty());
    }

    @Test
    void findServiceKeepsBranchThatOffersTheServiceAndHasABookableHour() {
        when(branchRepository.findActiveWithActivePartner()).thenReturn(List.of(baku, far));
        when(individualServiceSourceRepository.findIndividualServiceIdsByServiceId(144L)).thenReturn(List.of(501L));
        when(branchIndividualServiceRepository.findActivePricedBranchIds(501L)).thenReturn(List.of(7L, 8L));
        when(rangeRepository.findBranchIdsWithBookableIndividualSlot(
                eq(501L), eq("individual"), eq("AVAILABLE"), any(), eq("HIDDEN"),
                eq(BookingStatus.occupyingCapacity()))).thenReturn(List.of(7L));

        BookingBranchListResponse out = service.listActive(144L);

        assertEquals(1, out.getBranches().size());
        assertEquals(7L, out.getBranches().get(0).getBranchId());
    }

    @Test
    void findServiceWithNoCatalogLinkReturnsEmpty() {
        when(individualServiceSourceRepository.findIndividualServiceIdsByServiceId(999L)).thenReturn(List.of());
        when(branchRepository.findActiveWithActivePartner()).thenReturn(List.of(baku));

        BookingBranchListResponse out = service.listActive(999L);

        assertTrue(out.getBranches().isEmpty());
        verify(branchIndividualServiceRepository, never()).findActivePricedBranchIds(any());
        verify(rangeRepository, never()).findBranchIdsWithBookableIndividualSlot(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void findServiceDropsPricedBranchWhenNoBookableHour() {
        when(branchRepository.findActiveWithActivePartner()).thenReturn(List.of(baku));
        when(individualServiceSourceRepository.findIndividualServiceIdsByServiceId(144L)).thenReturn(List.of(501L));
        when(branchIndividualServiceRepository.findActivePricedBranchIds(501L)).thenReturn(List.of(7L));
        when(rangeRepository.findBranchIdsWithBookableIndividualSlot(
                eq(501L), eq("individual"), eq("AVAILABLE"), any(), eq("HIDDEN"),
                eq(BookingStatus.occupyingCapacity()))).thenReturn(List.of());

        BookingBranchListResponse out = service.listActive(144L);

        assertTrue(out.getBranches().isEmpty());
    }
}
