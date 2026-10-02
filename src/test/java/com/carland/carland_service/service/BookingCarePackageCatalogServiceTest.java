package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCarePackagesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchCarePackageItem;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCarePackageCatalogServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock BranchCarePackageRepository packageRepository;
    @Mock BranchCarePackageItemRepository itemRepository;

    BookingCarePackageCatalogService service;

    @BeforeEach
    void setUp() {
        service = new BookingCarePackageCatalogService(
                branchRepository, packageRepository, itemRepository, new ObjectMapper());
    }

    @Test
    void returnsActivePackagesAndEnabledServices() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        Branch branch = Branch.builder().id(12L).active(true).partner(partner).build();
        BranchCarePackage extra = BranchCarePackage.builder()
                .id(5L).branch(branch).name("Hyper extra").price(129).currency("AZN").active(true).build();
        BranchCarePackage closed = BranchCarePackage.builder()
                .id(6L).branch(branch).name("Kapalı").price(50).active(false).build();
        OfferedService oil = OfferedService.builder()
                .id(3L).titleJson("{\"az\":\"Yağ dəyişimi\",\"en\":\"Oil change\"}").build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(packageRepository.findByBranch_IdOrderByIdAsc(12L)).thenReturn(List.of(extra, closed));
        when(itemRepository.findByCarePackage_IdOrderByIdAsc(5L)).thenReturn(List.of(
                BranchCarePackageItem.builder().carePackage(extra).offeredService(oil).enabled(true).build(),
                BranchCarePackageItem.builder().carePackage(extra).offeredService(
                        OfferedService.builder().id(9L).titleJson("{\"az\":\"Gizli\"}").build()).enabled(false).build()
        ));

        BookingCarePackagesResponse out = service.list(12L, "az");

        assertEquals(12L, out.getBranchId());
        assertEquals(1, out.getPackages().size());
        assertEquals("Hyper extra", out.getPackages().get(0).getName());
        assertEquals(129, out.getPackages().get(0).getPrice());
        assertEquals(1, out.getPackages().get(0).getCount());
        assertEquals("Yağ dəyişimi", out.getPackages().get(0).getServices().get(0).getName());
        assertTrue(out.getPackages().stream().noneMatch(pkg -> pkg.getId().equals(6L)));
    }
}
