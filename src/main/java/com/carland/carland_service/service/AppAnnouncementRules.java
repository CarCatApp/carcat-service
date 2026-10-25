package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AppAnnouncementTranslationRequest;
import com.carland.carland_service.dto.request.AppAnnouncementWriteRequest;
import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.enums.AppAnnouncementType;
import com.carland.carland_service.util.AppVersion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * tr: Admin yazım kuralları. İhlal {@link IllegalArgumentException} olur ve 400 {@code error} gövdesine döner.
 * en: Admin write rules. A violation is {@link IllegalArgumentException} and becomes HTTP 400 {@code error}.
 */
public final class AppAnnouncementRules {

    private AppAnnouncementRules() {
    }

    public record Translation(String lang, String title, String body, String ctaLabel) {
    }

    public record Normalized(
            AppAnnouncementType type,
            boolean dismissible,
            AppAnnouncementPlatform platform,
            String minAppVersion,
            String maxAppVersion,
            String ctaUrl,
            String ctaUrlIos,
            String ctaUrlAndroid,
            int priority,
            Instant startsAt,
            Instant endsAt,
            List<Translation> translations
    ) {
    }

    public static Normalized validate(AppAnnouncementWriteRequest request, StoreLinks stores, Collection<String> hosts) {
        if (request == null) {
            throw new IllegalArgumentException("body is required");
        }
        AppAnnouncementType type = parseType(request.getType());
        boolean dismissible = request.getDismissible() == null || request.getDismissible();
        AppAnnouncementPlatform platform = parsePlatform(request.getPlatform());
        String min = versionOrNull(request.getMinAppVersion(), "minAppVersion");
        String max = versionOrNull(request.getMaxAppVersion(), "maxAppVersion");
        if (type == AppAnnouncementType.UPDATE && max == null) {
            throw new IllegalArgumentException("UPDATE requires maxAppVersion");
        }
        if (min != null && max != null) {
            AppVersion minVersion = AppVersion.parse(min).orElseThrow();
            AppVersion maxVersion = AppVersion.parse(max).orElseThrow();
            if (minVersion.compareTo(maxVersion) > 0) {
                throw new IllegalArgumentException("minAppVersion must be <= maxAppVersion");
            }
        }
        Instant startsAt = request.getStartsAt();
        Instant endsAt = request.getEndsAt();
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
        CtaUrls ctas = normalizeCtas(platform, request, hosts);
        List<Translation> translations = translations(request.getTranslations());
        if (type == AppAnnouncementType.UPDATE && !dismissible && !ctaResolvable(platform, ctas, stores)) {
            throw new IllegalArgumentException(
                    "non-dismissible UPDATE needs an https CTA on the allowlist or a store link for each target platform");
        }
        int priority = request.getPriority() == null ? 0 : request.getPriority();
        return new Normalized(
                type, dismissible, platform, min, max,
                ctas.shared(), ctas.ios(), ctas.android(),
                priority, startsAt, endsAt, translations);
    }

    public static boolean ctaResolvable(AppAnnouncementPlatform platform, String ctaUrl, StoreLinks stores) {
        return ctaResolvable(platform, new CtaUrls(ctaUrl, null, null), stores);
    }

    private static boolean ctaResolvable(AppAnnouncementPlatform platform, CtaUrls ctas, StoreLinks stores) {
        boolean iosLink = isSet(ctas.ios()) || isSet(ctas.shared());
        boolean androidLink = isSet(ctas.android()) || isSet(ctas.shared());
        boolean iosStore = stores != null && AppAnnouncementUrls.httpsOrNull(stores.ios()) != null;
        boolean androidStore = stores != null && AppAnnouncementUrls.httpsOrNull(stores.android()) != null;
        if (platform == AppAnnouncementPlatform.IOS) {
            return iosLink || iosStore;
        }
        if (platform == AppAnnouncementPlatform.ANDROID) {
            return androidLink || androidStore;
        }
        return (iosLink || iosStore) && (androidLink || androidStore);
    }

    /**
     * tr: ALL iki link saklar. Tek platforma eski {@code ctaUrl} alanında kalır.
     * en: ALL stores two links. A single platform stays on the legacy {@code ctaUrl} field.
     */
    private static CtaUrls normalizeCtas(
            AppAnnouncementPlatform platform,
            AppAnnouncementWriteRequest request,
            Collection<String> hosts
    ) {
        String shared = normalizeCta(request.getCtaUrl(), hosts, "ctaUrl");
        String ios = normalizeCta(request.getCtaUrlIos(), hosts, "ctaUrlIos");
        String android = normalizeCta(request.getCtaUrlAndroid(), hosts, "ctaUrlAndroid");
        if (platform == AppAnnouncementPlatform.IOS) {
            return new CtaUrls(first(ios, shared), null, null);
        }
        if (platform == AppAnnouncementPlatform.ANDROID) {
            return new CtaUrls(first(android, shared), null, null);
        }
        if (ios != null || android != null) {
            return new CtaUrls(null, ios, android);
        }
        return new CtaUrls(shared, null, null);
    }

    private static String first(String preferred, String fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private record CtaUrls(String shared, String ios, String android) {
    }

    private static List<Translation> translations(List<AppAnnouncementTranslationRequest> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("az translation is required");
        }
        List<Translation> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        boolean az = false;
        for (AppAnnouncementTranslationRequest row : raw) {
            if (row == null || row.getLang() == null || row.getLang().isBlank()) {
                throw new IllegalArgumentException("lang must be az, en, or ru");
            }
            String lang = row.getLang().trim().toLowerCase(Locale.ROOT);
            if (!lang.equals("az") && !lang.equals("en") && !lang.equals("ru")) {
                throw new IllegalArgumentException("lang must be az, en, or ru");
            }
            if (!seen.add(lang)) {
                throw new IllegalArgumentException("duplicate translation lang");
            }
            String title = requiredText(row.getTitle(), "title", 60);
            String body = requiredText(row.getBody(), "body", 500);
            String ctaLabel = optionalText(row.getCtaLabel(), "ctaLabel", 30);
            if ("az".equals(lang)) {
                az = true;
            }
            out.add(new Translation(lang, title, body, ctaLabel));
        }
        if (!az) {
            throw new IllegalArgumentException("az translation is required");
        }
        return List.copyOf(out);
    }

    private static String requiredText(String raw, String field, int max) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(field + " must be at most " + max + " characters");
        }
        return trimmed;
    }

    private static String optionalText(String raw, String field, int max) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(field + " must be at most " + max + " characters");
        }
        return trimmed;
    }

    private static String versionOrNull(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 32 || AppVersion.parse(trimmed).isEmpty()) {
            throw new IllegalArgumentException(field + " is not a valid version");
        }
        return trimmed;
    }

    private static String normalizeCta(String raw, Collection<String> hosts, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String allowed = AppAnnouncementUrls.allowlistedOrNull(raw, hosts);
        if (allowed == null) {
            throw new IllegalArgumentException(field + " must be https and on the host allowlist");
        }
        if (allowed.length() > 512) {
            throw new IllegalArgumentException(field + " must be at most 512 characters");
        }
        return allowed;
    }

    private static AppAnnouncementType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        try {
            return AppAnnouncementType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("type must be UPDATE, INFO, or MAINTENANCE");
        }
    }

    private static AppAnnouncementPlatform parsePlatform(String raw) {
        if (raw == null || raw.isBlank()) {
            return AppAnnouncementPlatform.ALL;
        }
        try {
            return AppAnnouncementPlatform.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("platform must be ALL, IOS, or ANDROID");
        }
    }
}
