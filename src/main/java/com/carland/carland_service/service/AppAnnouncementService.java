package com.carland.carland_service.service;

import com.carland.carland_service.config.AppAnnouncementProperties;
import com.carland.carland_service.dto.request.AppAnnouncementTranslationRequest;
import com.carland.carland_service.dto.request.AppAnnouncementWriteRequest;
import com.carland.carland_service.dto.response.AppAnnouncementActiveItem;
import com.carland.carland_service.dto.response.AppAnnouncementAdminResponse;
import com.carland.carland_service.entity.AppAnnouncement;
import com.carland.carland_service.entity.AppAnnouncementTranslation;
import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.enums.AppAnnouncementType;
import com.carland.carland_service.repository.AppAnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * tr: Aktif duyuru okuma ve admin yazma. Public seçim {@link AppAnnouncementSelection} ile aynıdır.
 * en: Active announcement reads and admin writes. Public selection is {@link AppAnnouncementSelection}.
 */
@Service
@RequiredArgsConstructor
public class AppAnnouncementService {

    private final AppAnnouncementRepository repository;
    private final AppAnnouncementCache cache;
    private final AppAnnouncementProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AppAnnouncementActiveItem> active(String acceptLanguage, String appVersion, String platform) {
        AppAnnouncementHeaders.Resolved resolved = AppAnnouncementHeaders.resolve(acceptLanguage, appVersion, platform);
        return select(resolved, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<AppAnnouncementActiveItem> preview(String version, String platform, String lang, Instant at) {
        AppAnnouncementHeaders.Resolved resolved = AppAnnouncementHeaders.resolve(lang, version, platform);
        Instant now = at == null ? clock.instant() : at;
        return select(resolved, now);
    }

    @Transactional(readOnly = true)
    public List<AppAnnouncementAdminResponse> list(Boolean active, AppAnnouncementType type, AppAnnouncementPlatform platform) {
        return distinct(repository.findAllWithTranslations()).stream()
                .filter(row -> active == null || row.isActive() == active)
                .filter(row -> type == null || row.getType() == type)
                .filter(row -> platform == null || row.getPlatform() == platform)
                .sorted(Comparator.comparing(AppAnnouncement::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toAdmin)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppAnnouncementAdminResponse get(long id) {
        return toAdmin(load(id));
    }

    @Transactional
    public AppAnnouncementAdminResponse create(AppAnnouncementWriteRequest request, String actor) {
        AppAnnouncementRules.Normalized normalized = AppAnnouncementRules.validate(request, stores(), hosts());
        AppAnnouncement entity = new AppAnnouncement();
        apply(entity, normalized);
        entity.setActive(false);
        entity.setCreatedAt(clock.instant());
        entity.setCreatedBy(blankActor(actor));
        replaceTranslations(entity, normalized.translations());
        AppAnnouncement saved = repository.save(entity);
        cache.evictAfterCommit();
        return toAdmin(saved);
    }

    @Transactional
    public AppAnnouncementAdminResponse update(long id, AppAnnouncementWriteRequest request, String actor) {
        AppAnnouncement entity = load(id);
        AppAnnouncementRules.Normalized normalized = AppAnnouncementRules.validate(request, stores(), hosts());
        apply(entity, normalized);
        replaceTranslations(entity, normalized.translations());
        entity.setUpdatedAt(clock.instant());
        entity.setUpdatedBy(blankActor(actor));
        cache.evictAfterCommit();
        return toAdmin(entity);
    }

    @Transactional
    public AppAnnouncementAdminResponse setActive(long id, boolean active, String actor) {
        AppAnnouncement entity = load(id);
        if (active) {
            AppAnnouncementRules.validate(toWriteRequest(entity), stores(), hosts());
        }
        entity.setActive(active);
        entity.setUpdatedAt(clock.instant());
        entity.setUpdatedBy(blankActor(actor));
        cache.evictAfterCommit();
        return toAdmin(entity);
    }

    @Transactional
    public AppAnnouncementAdminResponse copy(long id, String actor) {
        AppAnnouncement source = load(id);
        AppAnnouncementRules.Normalized normalized = AppAnnouncementRules.validate(toWriteRequest(source), stores(), hosts());
        AppAnnouncement copy = new AppAnnouncement();
        apply(copy, normalized);
        copy.setActive(false);
        copy.setCreatedAt(clock.instant());
        copy.setCreatedBy(blankActor(actor));
        replaceTranslations(copy, normalized.translations());
        AppAnnouncement saved = repository.save(copy);
        cache.evictAfterCommit();
        return toAdmin(saved);
    }

    private List<AppAnnouncementActiveItem> select(AppAnnouncementHeaders.Resolved resolved, Instant now) {
        List<AppAnnouncementCandidate> candidates = cache.getOrLoad(this::loadActiveCandidates);
        return AppAnnouncementSelection.select(candidates, resolved, now, stores(), hosts());
    }

    private List<AppAnnouncementCandidate> loadActiveCandidates() {
        return distinct(repository.findActiveWithTranslations()).stream()
                .map(AppAnnouncementCandidate::from)
                .toList();
    }

    private AppAnnouncement load(long id) {
        return repository.findWithTranslationsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found"));
    }

    private void apply(AppAnnouncement entity, AppAnnouncementRules.Normalized normalized) {
        entity.setType(normalized.type());
        entity.setDismissible(normalized.dismissible());
        entity.setPlatform(normalized.platform());
        entity.setMinAppVersion(normalized.minAppVersion());
        entity.setMaxAppVersion(normalized.maxAppVersion());
        entity.setCtaUrl(normalized.ctaUrl());
        entity.setCtaUrlIos(normalized.ctaUrlIos());
        entity.setCtaUrlAndroid(normalized.ctaUrlAndroid());
        entity.setPriority(normalized.priority());
        entity.setStartsAt(normalized.startsAt());
        entity.setEndsAt(normalized.endsAt());
    }

    private void replaceTranslations(AppAnnouncement entity, List<AppAnnouncementRules.Translation> translations) {
        if (entity.getTranslations() == null) {
            entity.setTranslations(new ArrayList<>());
        }
        Map<String, AppAnnouncementTranslation> existing = new LinkedHashMap<>();
        for (AppAnnouncementTranslation row : entity.getTranslations()) {
            if (row.getLang() != null) {
                existing.put(row.getLang(), row);
            }
        }
        Set<String> keep = translations.stream()
                .map(AppAnnouncementRules.Translation::lang)
                .collect(Collectors.toSet());
        for (AppAnnouncementRules.Translation translation : translations) {
            AppAnnouncementTranslation row = existing.get(translation.lang());
            if (row == null) {
                row = new AppAnnouncementTranslation();
                row.setAnnouncement(entity);
                row.setLang(translation.lang());
                entity.getTranslations().add(row);
            }
            row.setTitle(translation.title());
            row.setBody(translation.body());
            row.setCtaLabel(translation.ctaLabel());
        }
        entity.getTranslations().removeIf(row -> row.getLang() == null || !keep.contains(row.getLang()));
    }

    private AppAnnouncementWriteRequest toWriteRequest(AppAnnouncement entity) {
        AppAnnouncementWriteRequest request = new AppAnnouncementWriteRequest();
        request.setType(entity.getType() == null ? null : entity.getType().name());
        request.setDismissible(entity.isDismissible());
        request.setPlatform(entity.getPlatform() == null ? null : entity.getPlatform().name());
        request.setMinAppVersion(entity.getMinAppVersion());
        request.setMaxAppVersion(entity.getMaxAppVersion());
        request.setCtaUrl(entity.getCtaUrl());
        request.setCtaUrlIos(entity.getCtaUrlIos());
        request.setCtaUrlAndroid(entity.getCtaUrlAndroid());
        request.setPriority(entity.getPriority());
        request.setStartsAt(entity.getStartsAt());
        request.setEndsAt(entity.getEndsAt());
        List<AppAnnouncementTranslationRequest> translations = new ArrayList<>();
        if (entity.getTranslations() != null) {
            for (AppAnnouncementTranslation row : entity.getTranslations()) {
                AppAnnouncementTranslationRequest translation = new AppAnnouncementTranslationRequest();
                translation.setLang(row.getLang());
                translation.setTitle(row.getTitle());
                translation.setBody(row.getBody());
                translation.setCtaLabel(row.getCtaLabel());
                translations.add(translation);
            }
        }
        request.setTranslations(translations);
        return request;
    }

    private AppAnnouncementAdminResponse toAdmin(AppAnnouncement entity) {
        List<AppAnnouncementAdminResponse.Translation> translations = new ArrayList<>();
        if (entity.getTranslations() != null) {
            List<AppAnnouncementTranslation> rows = new ArrayList<>(entity.getTranslations());
            rows.sort(Comparator.comparing(row -> row.getLang() == null ? "" : row.getLang()));
            for (AppAnnouncementTranslation row : rows) {
                translations.add(AppAnnouncementAdminResponse.Translation.builder()
                        .lang(row.getLang())
                        .title(row.getTitle())
                        .body(row.getBody())
                        .ctaLabel(row.getCtaLabel())
                        .build());
            }
        }
        return AppAnnouncementAdminResponse.builder()
                .id(entity.getId() == null ? 0L : entity.getId())
                .type(entity.getType() == null ? null : entity.getType().name())
                .dismissible(entity.isDismissible())
                .platform(entity.getPlatform() == null ? null : entity.getPlatform().name())
                .minAppVersion(entity.getMinAppVersion())
                .maxAppVersion(entity.getMaxAppVersion())
                .ctaUrl(entity.getCtaUrl())
                .ctaUrlIos(entity.getCtaUrlIos())
                .ctaUrlAndroid(entity.getCtaUrlAndroid())
                .priority(entity.getPriority())
                .startsAt(entity.getStartsAt())
                .endsAt(entity.getEndsAt())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .translations(translations)
                .build();
    }

    private StoreLinks stores() {
        return new StoreLinks(properties.getIosStoreUrl(), properties.getAndroidStoreUrl());
    }

    private Set<String> hosts() {
        List<String> configured = properties.getCtaHostAllowlist();
        if (configured == null || configured.isEmpty()) {
            return Set.of();
        }
        return configured.stream()
                .filter(host -> host != null && !host.isBlank())
                .map(host -> host.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static List<AppAnnouncement> distinct(List<AppAnnouncement> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Long, AppAnnouncement> byId = new LinkedHashMap<>();
        for (AppAnnouncement row : rows) {
            if (row.getId() != null) {
                byId.putIfAbsent(row.getId(), row);
            }
        }
        return List.copyOf(byId.values());
    }

    private static String blankActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "unknown";
        }
        String trimmed = actor.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }
}
