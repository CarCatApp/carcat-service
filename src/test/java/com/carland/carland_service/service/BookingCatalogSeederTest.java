package com.carland.carland_service.service;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchPackage;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.repository.BranchPackageRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServiceBrandRepository;
import com.carland.carland_service.repository.BranchServiceRepository;
import com.carland.carland_service.repository.BookingCancelReasonRepository;
import com.carland.carland_service.repository.PartnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCatalogSeederTest {

    @Mock PartnerRepository partnerRepository;
    @Mock BranchRepository branchRepository;
    @Mock BranchPackageRepository packageRepository;
    @Mock BranchServiceRepository serviceRepository;
    @Mock BranchServiceBrandRepository brandRepository;
    @Mock BookingCancelReasonRepository cancelReasonRepository;

    @InjectMocks BookingCatalogSeeder seeder;

    @Test
    void seedsHyperExtraOnExistingHyperBranch() {
        Partner partner = Partner.builder().id(1L).name("HyperService").source("hyper").active(true).build();
        Branch branch = Branch.builder().id(12L).name("Babek").partner(partner).build();
        when(partnerRepository.findBySourceIgnoreCaseAndActiveTrue("hyper")).thenReturn(Optional.of(partner));
        when(branchRepository.findByPartnerOrderByIdAsc(partner)).thenReturn(List.of(branch));
        when(serviceRepository.existsByBranchIdAndServiceKey(anyLong(), anyString())).thenReturn(false);
        when(packageRepository.existsByBranchIdAndServiceKey(12L, "pkg:hyper-extra")).thenReturn(false);
        when(cancelReasonRepository.existsByCode(anyString())).thenReturn(false);
        when(serviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder.run(new DefaultApplicationArguments());

        ArgumentCaptor<BranchPackage> captor = ArgumentCaptor.forClass(BranchPackage.class);
        verify(packageRepository).save(captor.capture());
        assertEquals("pkg:hyper-extra", captor.getValue().getServiceKey());
        assertEquals(12900, captor.getValue().getPriceMin());
        assertEquals(12L, captor.getValue().getBranch().getId());
    }

    @Test
    void skipsWhenHyperPartnerMissing() {
        when(partnerRepository.findBySourceIgnoreCaseAndActiveTrue("hyper")).thenReturn(Optional.empty());
        when(cancelReasonRepository.existsByCode(anyString())).thenReturn(true);

        seeder.run(new DefaultApplicationArguments());

        verify(packageRepository, never()).save(any());
    }
}
