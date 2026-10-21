package com.carland.carland_service.service;

import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.util.AppVersion;

import java.util.Locale;
import java.util.Set;

/**
 * tr: Duyuru header'ları. Geçersiz değer 400 değildir: dil {@code az}'a düşer, platform ve sürüm bilinmiyor kalır.
 * en: Announcement headers. Invalid values are not 400: language falls back to {@code az},
 *     platform and version stay unknown.
 */
public final class AppAnnouncementHeaders {

    public static final int SHORT_HEADER_MAX = 32;
    public static final int LANGUAGE_HEADER_MAX = 128;

    private static final Set<String> LANGUAGES = Set.of("az", "en", "ru");

    private AppAnnouncementHeaders() {
    }

    public record Resolved(String lang, AppAnnouncementPlatform platform, AppVersion version) {
    }

    public static Resolved resolve(String acceptLanguage, String appVersion, String platform) {
        return new Resolved(language(acceptLanguage), platform(platform), version(appVersion));
    }

    public static String language(String header) {
        if (header == null) {
            return "az";
        }
        String trimmed = header.trim();
        if (trimmed.isEmpty() || trimmed.length() > LANGUAGE_HEADER_MAX) {
            return "az";
        }
        String first = trimmed;
        int comma = first.indexOf(',');
        if (comma >= 0) {
            first = first.substring(0, comma);
        }
        int semi = first.indexOf(';');
        if (semi >= 0) {
            first = first.substring(0, semi);
        }
        first = first.trim().toLowerCase(Locale.ROOT);
        int dash = first.indexOf('-');
        if (dash >= 0) {
            first = first.substring(0, dash);
        }
        int underscore = first.indexOf('_');
        if (underscore >= 0) {
            first = first.substring(0, underscore);
        }
        if (LANGUAGES.contains(first)) {
            return first;
        }
        return "az";
    }

    public static AppAnnouncementPlatform platform(String header) {
        if (header == null) {
            return null;
        }
        String trimmed = header.trim();
        if (trimmed.isEmpty() || trimmed.length() > SHORT_HEADER_MAX) {
            return null;
        }
        if (trimmed.equalsIgnoreCase("IOS")) {
            return AppAnnouncementPlatform.IOS;
        }
        if (trimmed.equalsIgnoreCase("ANDROID")) {
            return AppAnnouncementPlatform.ANDROID;
        }
        return null;
    }

    public static AppVersion version(String header) {
        if (header == null) {
            return null;
        }
        String trimmed = header.trim();
        if (trimmed.isEmpty() || trimmed.length() > SHORT_HEADER_MAX) {
            return null;
        }
        return AppVersion.parse(trimmed).orElse(null);
    }
}
