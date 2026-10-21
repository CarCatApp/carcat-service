package com.carland.carland_service.service;

import com.carland.carland_service.entity.AppAnnouncement;
import com.carland.carland_service.entity.AppAnnouncementTranslation;
import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.enums.AppAnnouncementType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Cache ve seçim için JPA'sız aday. Zaman filtresi bu nesnede durur, cache'e yazıldıktan sonra da uygulanır.
 * en: JPA-free candidate for cache and selection. The time window stays on this object and is applied after a cache hit.
 */
public record AppAnnouncementCandidate(
        long id,
        AppAnnouncementType type,
        boolean dismissible,
        AppAnnouncementPlatform platform,
        String minAppVersion,
        String maxAppVersion,
        String ctaUrl,
        int priority,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt,
        boolean active,
        List<Text> translations
) {
    public record Text(String lang, String title, String body, String ctaLabel) {
    }

    public static AppAnnouncementCandidate from(AppAnnouncement entity) {
        List<Text> texts = new ArrayList<>();
        if (entity.getTranslations() != null) {
            for (AppAnnouncementTranslation translation : entity.getTranslations()) {
                texts.add(new Text(
                        translation.getLang(),
                        translation.getTitle(),
                        translation.getBody(),
                        translation.getCtaLabel()
                ));
            }
        }
        return new AppAnnouncementCandidate(
                entity.getId(),
                entity.getType(),
                entity.isDismissible(),
                entity.getPlatform(),
                entity.getMinAppVersion(),
                entity.getMaxAppVersion(),
                entity.getCtaUrl(),
                entity.getPriority(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getCreatedAt(),
                entity.isActive(),
                List.copyOf(texts)
        );
    }
}
