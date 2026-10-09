package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCarePackageGroupView;
import com.carland.carland_service.dto.booking.BookingCarePackageServiceView;
import com.carland.carland_service.dto.booking.BookingCarePackagesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchCarePackageItem;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.ServiceBehavior;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.OfferedServicePhotoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCarePackageCatalogServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock BranchCarePackageRepository packageRepository;
    @Mock BranchCarePackageItemRepository itemRepository;
    @Mock OfferedServicePhotoRepository photoRepository;

    BookingCarePackageCatalogService service;

    @BeforeEach
    void setUp() {
        service = new BookingCarePackageCatalogService(
                branchRepository, packageRepository, itemRepository, photoRepository, new ObjectMapper());
    }

    @Test
    void groupsEnabledServicesByBehaviorAndAttachesIcon() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        Branch branch = Branch.builder().id(12L).active(true).partner(partner).build();
        BranchCarePackage extra = BranchCarePackage.builder()
                .id(5L).branch(branch).name("Hyper extra").price(129).currency("AZN").active(true).build();
        BranchCarePackage closed = BranchCarePackage.builder()
                .id(6L).branch(branch).name("Kapalı").price(50).active(false).build();
        ServiceBehavior replace = ServiceBehavior.builder()
                .id(30L).code("replace").sortOrder(1)
                .titleJson("{\"az\":\"DƏYİŞDİRMƏ\",\"en\":\"Replace\",\"ru\":\"\"}").build();
        ServiceBehavior topUp = ServiceBehavior.builder()
                .id(20L).code("extra").sortOrder(2)
                .titleJson("{\"az\":\"ƏLAVƏ ETMƏ\",\"en\":\"Top up\",\"ru\":\"\"}").build();
        OfferedService oil = OfferedService.builder()
                .id(3L).sortOrder(2).behavior(topUp)
                .titleJson("{\"az\":\"Yağ dəyişimi\",\"en\":\"Oil change\"}").build();
        OfferedService washer = OfferedService.builder()
                .id(6L).sortOrder(1).behavior(topUp)
                .titleJson("{\"az\":\"Şüşəyuyan mayesi\",\"en\":\"Washer fluid\"}").build();
        OfferedService pads = OfferedService.builder()
                .id(8L).sortOrder(1).behavior(replace)
                .titleJson("{\"az\":\"Əyləc qəlibi\",\"en\":\"Brake pads\"}").build();
        OfferedService hidden = OfferedService.builder()
                .id(9L).behavior(topUp).titleJson("{\"az\":\"Gizli\"}").build();
        OfferedService ungrouped = OfferedService.builder()
                .id(11L).titleJson("{\"az\":\"Davranışsız\"}").build();
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(packageRepository.findByBranch_IdOrderByIdAsc(12L)).thenReturn(List.of(extra, closed));
        when(itemRepository.findByCarePackage_IdOrderByIdAsc(5L)).thenReturn(List.of(
                BranchCarePackageItem.builder().carePackage(extra).offeredService(oil).enabled(true).build(),
                BranchCarePackageItem.builder().carePackage(extra).offeredService(hidden).enabled(false).build(),
                BranchCarePackageItem.builder().carePackage(extra).offeredService(ungrouped).enabled(true).build(),
                BranchCarePackageItem.builder().carePackage(extra).offeredService(pads).enabled(true).build(),
                BranchCarePackageItem.builder().carePackage(extra).offeredService(washer).enabled(true).build()
        ));
        when(photoRepository.findOfferedServiceIdsWithImage(anyCollection())).thenReturn(List.of(8L));

        BookingCarePackagesResponse az = service.list(12L, "az");

        assertEquals(12L, az.getBranchId());
        assertEquals(1, az.getPackages().size());
        assertEquals("Hyper extra", az.getPackages().get(0).getName());
        assertEquals(129, az.getPackages().get(0).getPrice());
        assertEquals(3, az.getPackages().get(0).getCount());
        assertTrue(az.getPackages().stream().noneMatch(pkg -> pkg.getId().equals(6L)));
        List<BookingCarePackageGroupView> groups = az.getPackages().get(0).getGroups();
        assertEquals(List.of(30L, 20L), groups.stream().map(BookingCarePackageGroupView::getId).toList());
        assertEquals("DƏYİŞDİRMƏ", groups.get(0).getName());
        assertEquals("ƏLAVƏ ETMƏ", groups.get(1).getName());
        BookingCarePackageServiceView padsView = groups.get(0).getServices().get(0);
        assertEquals("Əyləc qəlibi", padsView.getName());
        assertEquals(BookingCarePackageCatalogService.ICON_PATH + "8", padsView.getIconUrl());
        assertEquals(List.of(6L, 3L), groups.get(1).getServices().stream()
                .map(BookingCarePackageServiceView::getId).toList());
        assertEquals("Şüşəyuyan mayesi", groups.get(1).getServices().get(0).getName());
        assertNull(groups.get(1).getServices().get(0).getIconUrl());

        when(itemRepository.findByCarePackage_IdOrderByIdAsc(5L)).thenReturn(List.of(
                BranchCarePackageItem.builder().carePackage(extra).offeredService(oil).enabled(true).build()
        ));
        when(photoRepository.findOfferedServiceIdsWithImage(anyCollection())).thenReturn(List.of());
        BookingCarePackagesResponse en = service.list(12L, "en");
        assertEquals("Top up", en.getPackages().get(0).getGroups().get(0).getName());
        assertEquals("Oil change", en.getPackages().get(0).getGroups().get(0).getServices().get(0).getName());
    }
}
