package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffIndividualServiceListResponse;
import com.carland.carland_service.dto.booking.StaffIndividualServiceView;
import com.carland.carland_service.dto.request.StaffIndividualServicePricesRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.IndividualServiceFilterRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndividualCatalogServiceTest {

    @Mock IndividualServiceRepository individualServiceRepository;
    @Mock IndividualServiceFilterRepository individualServiceFilterRepository;
    @Mock BranchIndividualServiceRepository branchServiceRepository;
    @Mock BranchRepository branchRepository;
    @Mock BookingStaffAccess bookingStaffAccess;

    IndividualCatalogService service;
    BookingStaff staff;
    Branch branch;
    IndividualService catalog;
    BranchIndividualService offer;

    @BeforeEach
    void setUp() {
        service = new IndividualCatalogService(
                individualServiceRepository,
                individualServiceFilterRepository,
                branchServiceRepository,
                branchRepository,
                bookingStaffAccess,
                new ServiceCategoryJson(new ObjectMapper()));
        Partner partner = Partner.builder().id(3L).build();
        branch = Branch.builder().id(12L).partner(partner).build();
        staff = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .role(BookingStaffRole.PARTNER_ADMIN.name())
                .build();
        catalog = IndividualService.builder()
                .id(4L)
                .code("RZV")
                .titleJson("{\"az\":\"Razval\",\"en\":\"Wheel alignment\",\"ru\":\"Развал-схождение\"}")
                .sortOrder(5)
                .active(true)
                .build();
        offer = BranchIndividualService.builder()
                .id(8L)
                .branch(branch)
                .individualService(catalog)
                .active(true)
                .priceSimple(20)
                .priceMedium(25)
                .priceComplex(30)
                .build();
    }

    @Test
    void listKeepsPricesWhenBranchRowIsOff() {
        offer.setActive(false);
        when(bookingStaffAccess.requireStaff(9L, false, "az")).thenReturn(staff);
        when(branchRepository.findByPartnerOrderByIdAsc(staff.getPartner())).thenReturn(List.of(branch));
        when(individualServiceRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(catalog));
        when(branchServiceRepository.findByBranch_Id(12L)).thenReturn(List.of(offer));

        StaffIndividualServiceListResponse body = service.listForStaff(9L, false, null, "az");

        StaffIndividualServiceView row = body.getServices().get(0);
        assertEquals(12L, body.getBranchId());
        assertFalse(row.getActive());
        assertEquals(20, row.getPriceSimple());
        assertEquals(25, row.getPriceMedium());
        assertEquals(30, row.getPriceComplex());
    }

    @Test
    void setActiveOffDoesNotClearPrices() {
        when(bookingStaffAccess.requireStaff(9L, false, "az")).thenReturn(staff);
        when(branchRepository.findByPartnerOrderByIdAsc(staff.getPartner())).thenReturn(List.of(branch));
        when(individualServiceRepository.findById(4L)).thenReturn(Optional.of(catalog));
        when(branchServiceRepository.findByBranch_IdAndIndividualService_Id(12L, 4L)).thenReturn(Optional.of(offer));
        when(branchServiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffIndividualServiceView saved = service.setActive(9L, false, 4L, false, "az");

        ArgumentCaptor<BranchIndividualService> captor = ArgumentCaptor.forClass(BranchIndividualService.class);
        verify(branchServiceRepository).save(captor.capture());
        assertFalse(captor.getValue().getActive());
        assertEquals(20, captor.getValue().getPriceSimple());
        assertEquals(30, captor.getValue().getPriceComplex());
        assertFalse(saved.getActive());
        assertEquals(25, saved.getPriceMedium());
    }

    @Test
    void updatePricesDoesNotChangeActive() {
        when(bookingStaffAccess.requireStaff(9L, false, "az")).thenReturn(staff);
        when(branchRepository.findByPartnerOrderByIdAsc(staff.getPartner())).thenReturn(List.of(branch));
        when(individualServiceRepository.findById(4L)).thenReturn(Optional.of(catalog));
        when(branchServiceRepository.findByBranch_IdAndIndividualService_Id(12L, 4L)).thenReturn(Optional.of(offer));
        when(branchServiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffIndividualServiceView saved = service.updatePrices(9L, false, 4L,
                StaffIndividualServicePricesRequest.builder()
                        .priceSimple(20)
                        .priceMedium(40)
                        .priceComplex(null)
                        .build(),
                "az");

        assertTrue(saved.getActive());
        assertEquals(40, saved.getPriceMedium());
        assertNull(saved.getPriceComplex());
        assertEquals(20, saved.getPriceSimple());
    }
}
