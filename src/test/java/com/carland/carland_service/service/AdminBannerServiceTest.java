package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminBannerSaveRequest;
import com.carland.carland_service.dto.response.AdminBannerBranchOption;
import com.carland.carland_service.dto.response.AdminBannerRow;
import com.carland.carland_service.dto.response.BannerItem;
import com.carland.carland_service.entity.Banner;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BannerRepository;
import com.carland.carland_service.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBannerServiceTest {

    @Mock BannerRepository bannerRepository;
    @Mock BranchRepository branchRepository;
    @Mock PhotoService photoService;

    AdminBannerService service;

    @BeforeEach
    void setUp() {
        service = new AdminBannerService(bannerRepository, branchRepository, photoService);
    }

    @Test
    void createIgnoresShowingAndStatusFromTheClient() {
        when(bannerRepository.save(any())).thenAnswer(invocation -> {
            Banner banner = invocation.getArgument(0);
            banner.setId(7L);
            return banner;
        });
        AdminBannerSaveRequest body = body("hyper babek prospekti", "  aci ", 99L);
        body.setShowing(true);
        body.setStatus(Banner.DEACTIVE);

        AdminBannerRow row = service.create(body);

        ArgumentCaptor<Banner> saved = ArgumentCaptor.forClass(Banner.class);
        verify(bannerRepository).save(saved.capture());
        assertFalse(saved.getValue().getShowing());
        assertEquals(Banner.ACTIVE, saved.getValue().getStatus());
        assertEquals(Banner.SERVICE, saved.getValue().getType());
        assertEquals(0, saved.getValue().getSortOrder());
        assertEquals("hyper babek prospekti", row.getTitle());
        assertEquals("aci", row.getDescription());
        assertEquals(99L, row.getBranchId());
        assertFalse(row.getShowing());
        assertEquals(Banner.ACTIVE, row.getStatus());
        verifyNoInteractions(branchRepository);
    }

    @Test
    void shortDescriptionIsRejected() {
        AdminBannerSaveRequest body = body("Babek", "ab", 1L);

        assertThrows(MissingFieldException.class, () -> service.create(body));
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void descriptionOfThreeCharactersIsStored() {
        when(bannerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminBannerRow row = service.create(body("Babek", "  abc", 4L));

        assertEquals("abc", row.getDescription());
    }

    @Test
    void mobileListIsShowingAndActive() {
        Banner banner = Banner.builder()
                .id(4L)
                .title("hyper babek prospekti")
                .description("Yağ dəyişimi")
                .branchId(12L)
                .showing(true)
                .status(Banner.ACTIVE)
                .build();
        when(bannerRepository.findVisible(Banner.ACTIVE)).thenReturn(List.of(banner));

        List<BannerItem> rows = service.visible();

        assertEquals(1, rows.size());
        assertEquals(4L, rows.get(0).getId());
        assertEquals("hyper babek prospekti", rows.get(0).getTitle());
        assertEquals("Yağ dəyişimi", rows.get(0).getDescription());
        assertEquals(12L, rows.get(0).getBranchId());
        assertEquals(Banner.SERVICE, rows.get(0).getType());
        assertTrue(rows.get(0).isShowing());
        assertEquals(Banner.ACTIVE, rows.get(0).getStatus());
        verify(bannerRepository).findVisible(Banner.ACTIVE);
    }

    @Test
    void listReadsActiveOnly() {
        when(bannerRepository.findActiveOrdered(Banner.ACTIVE)).thenReturn(List.of());

        service.active();

        verify(bannerRepository).findActiveOrdered(Banner.ACTIVE);
    }

    @Test
    void branchListIsNamesSortedAndDoesNotCarryPartner() {
        when(branchRepository.findActiveWithActivePartner()).thenReturn(List.of(
                Branch.builder().id(2L).name("Zebra").build(),
                Branch.builder().id(1L).name("Babek").build()
        ));

        List<AdminBannerBranchOption> options = service.branches();

        assertEquals(2, options.size());
        assertEquals("Babek", options.get(0).getName());
        assertEquals(1L, options.get(0).getId());
        assertEquals("Zebra", options.get(1).getName());
    }

    @Test
    void turningShowingOnLeavesTheOtherBannerShowing() {
        Banner chosen = stored(false);
        Banner other = Banner.builder()
                .id(2L)
                .title("Eski")
                .description("abc")
                .branchId(8L)
                .showing(true)
                .status(Banner.ACTIVE)
                .build();
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(chosen));
        when(bannerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AdminBannerSaveRequest body = new AdminBannerSaveRequest();
        body.setShowing(true);

        AdminBannerRow row = service.update(1L, body);

        assertTrue(row.getShowing());
        assertTrue(other.getShowing());
        assertEquals("Babek", row.getTitle());
        assertEquals(Banner.ACTIVE, row.getStatus());
    }

    @Test
    void openingTheListKeepsEveryShowingBanner() {
        Banner newer = Banner.builder()
                .id(5L)
                .title("Yeni")
                .description("abc")
                .branchId(1L)
                .showing(true)
                .status(Banner.ACTIVE)
                .build();
        Banner older = Banner.builder()
                .id(2L)
                .title("Eski")
                .description("abc")
                .branchId(1L)
                .showing(true)
                .status(Banner.ACTIVE)
                .build();
        when(bannerRepository.findActiveOrdered(Banner.ACTIVE)).thenReturn(List.of(older, newer));

        List<AdminBannerRow> rows = service.active();

        assertTrue(newer.getShowing());
        assertTrue(older.getShowing());
        assertEquals(2, rows.size());
    }

    @Test
    void mobileListReturnsEveryShowingBannerInRepositoryOrder() {
        Banner first = Banner.builder()
                .id(2L)
                .title("Eski")
                .description("abc")
                .branchId(1L)
                .sortOrder(1)
                .showing(true)
                .status(Banner.ACTIVE)
                .build();
        Banner second = Banner.builder()
                .id(5L)
                .title("Yeni")
                .description("abc")
                .type(Banner.ADDS)
                .url("https://carland.az")
                .sortOrder(2)
                .showing(true)
                .status(Banner.ACTIVE)
                .build();
        when(bannerRepository.findVisible(Banner.ACTIVE)).thenReturn(List.of(first, second));

        List<BannerItem> rows = service.visible();

        assertEquals(2, rows.size());
        assertEquals(2L, rows.get(0).getId());
        assertEquals(5L, rows.get(1).getId());
        assertEquals(Banner.ADDS, rows.get(1).getType());
        assertEquals("https://carland.az", rows.get(1).getUrl());
    }

    @Test
    void setShowingLeavesTheOtherFields() {
        Banner existing = stored(true);
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bannerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AdminBannerSaveRequest body = new AdminBannerSaveRequest();
        body.setShowing(false);

        AdminBannerRow row = service.update(1L, body);

        assertFalse(row.getShowing());
        assertEquals("Babek", row.getTitle());
        assertEquals(3L, row.getBranchId());
        assertEquals(Banner.ACTIVE, row.getStatus());
    }

    @Test
    void editChangesTitleAndBranchIdWithoutResettingShowing() {
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(stored(true)));
        when(bannerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AdminBannerSaveRequest body = body("hyper babek prospekti", "yeni metn", 50L);
        body.setShowing(false);

        AdminBannerRow row = service.update(1L, body);

        assertEquals("hyper babek prospekti", row.getTitle());
        assertEquals(50L, row.getBranchId());
        assertTrue(row.getShowing());
        assertEquals(Banner.ACTIVE, row.getStatus());
        verifyNoInteractions(branchRepository);
    }

    @Test
    void deleteSetsDeactiveAndKeepsTheText() {
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(stored(false)));
        when(bannerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AdminBannerSaveRequest body = new AdminBannerSaveRequest();
        body.setStatus(Banner.DEACTIVE);

        AdminBannerRow row = service.update(1L, body);

        assertEquals(Banner.DEACTIVE, row.getStatus());
        assertEquals("Babek", row.getTitle());
        assertFalse(row.getShowing());
    }

    @Test
    void panelCannotSetStatusActive() {
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(stored(false)));
        AdminBannerSaveRequest body = new AdminBannerSaveRequest();
        body.setStatus(Banner.ACTIVE);

        assertThrows(InvalidStatusException.class, () -> service.update(1L, body));
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void missingBannerIsNotFound() {
        when(bannerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(9L, body("A", "abc", 1L)));
    }

    @Test
    void addsCreateStoresOptionalUrlAndRequiresAPhoto() {
        when(bannerRepository.save(any())).thenAnswer(invocation -> {
            Banner banner = invocation.getArgument(0);
            banner.setId(8L);
            return banner;
        });
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        AdminBannerSaveRequest body = new AdminBannerSaveRequest();
        body.setType(Banner.ADDS);
        body.setTitle("Kampanya");
        body.setDescription("yaz kampanyasi");
        body.setUrl("  ");
        body.setSortOrder(3);

        AdminBannerRow row = service.create(body, file);

        assertEquals(Banner.ADDS, row.getType());
        assertEquals(null, row.getBranchId());
        assertEquals(null, row.getUrl());
        assertEquals(3, row.getSortOrder());
        verify(photoService).replaceBannerPhoto(file, 8L);
    }

    @Test
    void addsWithoutPhotoIsRejected() {
        AdminBannerSaveRequest body = new AdminBannerSaveRequest();
        body.setType(Banner.INFO);
        body.setTitle("Melumat");
        body.setDescription("qisa metn");

        assertThrows(MissingFieldException.class, () -> service.create(body, null));
    }

    private static AdminBannerSaveRequest body(String title, String description, Long branchId) {
        AdminBannerSaveRequest body = new AdminBannerSaveRequest();
        body.setTitle(title);
        body.setDescription(description);
        body.setBranchId(branchId);
        return body;
    }

    private static Banner stored(boolean showing) {
        return Banner.builder()
                .id(1L)
                .title("Babek")
                .description("abc")
                .branchId(3L)
                .showing(showing)
                .status(Banner.ACTIVE)
                .build();
    }
}
