package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminBannerSaveRequest;
import com.carland.carland_service.dto.response.AdminBannerBranchOption;
import com.carland.carland_service.dto.response.AdminBannerRow;
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

    AdminBannerService service;

    @BeforeEach
    void setUp() {
        service = new AdminBannerService(bannerRepository, branchRepository);
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
    void listReadsActiveOnly() {
        when(bannerRepository.findByStatusOrderByIdDesc(Banner.ACTIVE)).thenReturn(List.of());

        service.active();

        verify(bannerRepository).findByStatusOrderByIdDesc(Banner.ACTIVE);
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
