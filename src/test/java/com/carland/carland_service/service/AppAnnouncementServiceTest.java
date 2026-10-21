package com.carland.carland_service.service;

import com.carland.carland_service.config.AppAnnouncementProperties;
import com.carland.carland_service.dto.request.AppAnnouncementTranslationRequest;
import com.carland.carland_service.dto.request.AppAnnouncementWriteRequest;
import com.carland.carland_service.dto.response.AppAnnouncementAdminResponse;
import com.carland.carland_service.entity.AppAnnouncement;
import com.carland.carland_service.entity.AppAnnouncementTranslation;
import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.enums.AppAnnouncementType;
import com.carland.carland_service.repository.AppAnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppAnnouncementServiceTest {

    private static final Instant T = Instant.parse("2026-06-01T12:00:00Z");

    @Mock
    private AppAnnouncementRepository repository;

    @Mock
    private AppAnnouncementCache cache;

    @Mock
    private AppAnnouncementProperties properties;

    private AppAnnouncementService service;

    @BeforeEach
    void setUp() {
        service = new AppAnnouncementService(repository, cache, properties, Clock.fixed(T, ZoneOffset.UTC));
    }

    @Test
    void createStaysInactiveAndEvictsAfterCommit() {
        when(properties.getCtaHostAllowlist()).thenReturn(List.of("play.google.com"));
        when(properties.getIosStoreUrl()).thenReturn("");
        when(properties.getAndroidStoreUrl()).thenReturn("");
        when(repository.save(any())).thenAnswer(invocation -> {
            AppAnnouncement saved = invocation.getArgument(0);
            assertNull(saved.getId());
            assertFalse(saved.isActive());
            saved.setId(9L);
            return saved;
        });

        AppAnnouncementAdminResponse response = service.create(infoRequest(), "994501112233");

        assertEquals(9L, response.getId());
        assertFalse(response.isActive());
        assertEquals("Salam", response.getTranslations().get(0).getTitle());
        verify(cache).evictAfterCommit();
    }

    @Test
    void copyGetsNewIdAndStaysInactive() {
        when(properties.getCtaHostAllowlist()).thenReturn(List.of());
        when(properties.getIosStoreUrl()).thenReturn("");
        when(properties.getAndroidStoreUrl()).thenReturn("");
        when(repository.findWithTranslationsById(3L)).thenReturn(Optional.of(stored(3L, true)));
        when(repository.save(any())).thenAnswer(invocation -> {
            AppAnnouncement saved = invocation.getArgument(0);
            saved.setId(8L);
            return saved;
        });

        AppAnnouncementAdminResponse response = service.copy(3L, "panel");

        assertEquals(8L, response.getId());
        assertFalse(response.isActive());
        verify(cache).evictAfterCommit();
    }

    @Test
    void previewUsesSuppliedInstant() {
        when(properties.getCtaHostAllowlist()).thenReturn(List.of());
        when(properties.getIosStoreUrl()).thenReturn("");
        when(properties.getAndroidStoreUrl()).thenReturn("");
        AppAnnouncementCandidate visibleAtT = new AppAnnouncementCandidate(
                4L,
                AppAnnouncementType.INFO,
                true,
                AppAnnouncementPlatform.ALL,
                null,
                null,
                null,
                0,
                T,
                null,
                T,
                true,
                List.of(new AppAnnouncementCandidate.Text("az", "Açıq", "Mətn", null))
        );
        when(cache.getOrLoad(any())).thenReturn(List.of(visibleAtT));

        assertEquals(1, service.preview(null, null, "az", T).size());
        assertEquals(0, service.preview(null, null, "az", T.minusSeconds(1)).size());
    }

    private static AppAnnouncementWriteRequest infoRequest() {
        AppAnnouncementTranslationRequest translation = new AppAnnouncementTranslationRequest();
        translation.setLang("az");
        translation.setTitle("Salam");
        translation.setBody("Mətn");
        AppAnnouncementWriteRequest request = new AppAnnouncementWriteRequest();
        request.setType("INFO");
        request.setTranslations(List.of(translation));
        return request;
    }

    private static AppAnnouncement stored(long id, boolean active) {
        AppAnnouncement announcement = new AppAnnouncement();
        announcement.setId(id);
        announcement.setType(AppAnnouncementType.INFO);
        announcement.setDismissible(true);
        announcement.setPlatform(AppAnnouncementPlatform.ALL);
        announcement.setActive(active);
        announcement.setCreatedAt(T);
        AppAnnouncementTranslation translation = new AppAnnouncementTranslation();
        translation.setLang("az");
        translation.setTitle("Salam");
        translation.setBody("Mətn");
        translation.setAnnouncement(announcement);
        announcement.setTranslations(new ArrayList<>(List.of(translation)));
        return announcement;
    }
}
