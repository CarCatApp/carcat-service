package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.AppAnnouncementActiveItem;
import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.enums.AppAnnouncementType;
import com.carland.carland_service.util.AppVersion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * tr: Duyuru seçimi. Sıra: kapatılamaz, tür, priority, createdAt, id. En fazla 5.
 * en: Announcement selection. Order: non-dismissible, type, priority, createdAt, id. At most 5.
 */
public final class AppAnnouncementSelection {

    private static final int LIMIT = 5;

    private AppAnnouncementSelection() {
    }

    public static List<AppAnnouncementActiveItem> select(
            List<AppAnnouncementCandidate> candidates,
            AppAnnouncementHeaders.Resolved request,
            Instant now,
            StoreLinks stores,
            Collection<String> ctaHosts
    ) {
        if (candidates == null || candidates.isEmpty() || now == null) {
            return List.of();
        }
        AppAnnouncementHeaders.Resolved ctx = request == null
                ? new AppAnnouncementHeaders.Resolved("az", null, null)
                : request;
        List<Match> matches = new ArrayList<>();
        for (AppAnnouncementCandidate candidate : candidates) {
            if (!candidate.active() || !inWindow(candidate, now)) {
                continue;
            }
            if (!platformMatches(candidate.platform(), ctx.platform())) {
                continue;
            }
            if (!versionMatches(candidate, ctx.version())) {
                continue;
            }
            AppAnnouncementCandidate.Text text = resolveText(candidate, ctx.lang());
            if (text == null) {
                continue;
            }
            ResolvedCtas ctas = resolveCtas(candidate, ctx.platform(), stores, ctaHosts);
            if (candidate.type() == AppAnnouncementType.UPDATE && ctas.selected() == null) {
                continue;
            }
            matches.add(new Match(candidate, text, ctas));
        }
        matches.sort(ORDER);
        if (matches.size() > LIMIT) {
            matches = matches.subList(0, LIMIT);
        }
        List<AppAnnouncementActiveItem> items = new ArrayList<>(matches.size());
        for (Match match : matches) {
            items.add(AppAnnouncementActiveItem.builder()
                    .id(match.candidate().id())
                    .type(match.candidate().type().name())
                    .dismissible(match.candidate().dismissible())
                    .title(match.text().title().trim())
                    .body(match.text().body().trim())
                    .ctaLabel(blankToNull(match.text().ctaLabel()))
                    .ctaUrl(match.ctas().selected())
                    .ctaUrlIos(match.ctas().ios())
                    .ctaUrlAndroid(match.ctas().android())
                    .build());
        }
        return List.copyOf(items);
    }

    private static boolean inWindow(AppAnnouncementCandidate candidate, Instant now) {
        if (candidate.startsAt() != null && candidate.startsAt().isAfter(now)) {
            return false;
        }
        return candidate.endsAt() == null || now.isBefore(candidate.endsAt());
    }

    private static boolean platformMatches(AppAnnouncementPlatform recordPlatform, AppAnnouncementPlatform requestPlatform) {
        if (recordPlatform == null || recordPlatform == AppAnnouncementPlatform.ALL) {
            return true;
        }
        return recordPlatform == requestPlatform;
    }

    private static boolean versionMatches(AppAnnouncementCandidate candidate, AppVersion requestVersion) {
        boolean minSet = isSet(candidate.minAppVersion());
        boolean maxSet = isSet(candidate.maxAppVersion());
        if (requestVersion == null) {
            return !minSet && !maxSet;
        }
        if (minSet) {
            AppVersion min = AppVersion.parse(candidate.minAppVersion()).orElse(null);
            if (min == null || requestVersion.compareTo(min) < 0) {
                return false;
            }
        }
        if (maxSet) {
            AppVersion max = AppVersion.parse(candidate.maxAppVersion()).orElse(null);
            if (max == null || requestVersion.compareTo(max) > 0) {
                return false;
            }
        }
        return true;
    }

    private static AppAnnouncementCandidate.Text resolveText(AppAnnouncementCandidate candidate, String lang) {
        String wanted = lang == null || lang.isBlank() ? "az" : lang;
        AppAnnouncementCandidate.Text requested = findUsable(candidate, wanted);
        if (requested != null) {
            return requested;
        }
        if (!"az".equals(wanted)) {
            return findUsable(candidate, "az");
        }
        return null;
    }

    private static AppAnnouncementCandidate.Text findUsable(AppAnnouncementCandidate candidate, String lang) {
        if (candidate.translations() == null) {
            return null;
        }
        for (AppAnnouncementCandidate.Text text : candidate.translations()) {
            if (text == null || text.lang() == null || !text.lang().equalsIgnoreCase(lang)) {
                continue;
            }
            if (isSet(text.title()) && isSet(text.body())) {
                return text;
            }
        }
        return null;
    }

    /**
     * tr: {@code selected} isteyen platformun linkidir ve eski {@code ctaUrl} alanıdır.
     *     {@code ios} / {@code android} her zaman döner; ALL kaydında iki kolon da doluysa ikisi de gelir.
     * en: {@code selected} is the requesting platform's link and stays on the legacy {@code ctaUrl} field.
     *     {@code ios} / {@code android} are always returned; an ALL row with both columns sends both.
     */
    private static ResolvedCtas resolveCtas(
            AppAnnouncementCandidate candidate,
            AppAnnouncementPlatform requestPlatform,
            StoreLinks stores,
            Collection<String> ctaHosts
    ) {
        String ios = sideUrl(candidate, AppAnnouncementPlatform.IOS, stores, ctaHosts);
        String android = sideUrl(candidate, AppAnnouncementPlatform.ANDROID, stores, ctaHosts);
        String selected = requestPlatform == AppAnnouncementPlatform.IOS
                ? ios
                : requestPlatform == AppAnnouncementPlatform.ANDROID
                ? android
                : AppAnnouncementUrls.allowlistedOrNull(candidate.ctaUrl(), ctaHosts);
        return new ResolvedCtas(selected, ios, android);
    }

    private static String sideUrl(
            AppAnnouncementCandidate candidate,
            AppAnnouncementPlatform side,
            StoreLinks stores,
            Collection<String> ctaHosts
    ) {
        if (candidate.platform() != null
                && candidate.platform() != AppAnnouncementPlatform.ALL
                && candidate.platform() != side) {
            return null;
        }
        String column = side == AppAnnouncementPlatform.IOS ? candidate.ctaUrlIos() : candidate.ctaUrlAndroid();
        String explicit = AppAnnouncementUrls.allowlistedOrNull(column, ctaHosts);
        if (explicit != null) {
            return explicit;
        }
        String shared = AppAnnouncementUrls.allowlistedOrNull(candidate.ctaUrl(), ctaHosts);
        if (shared != null && (candidate.platform() == side || bothColumnsEmpty(candidate))) {
            return shared;
        }
        if (candidate.type() != AppAnnouncementType.UPDATE || stores == null) {
            return null;
        }
        String store = side == AppAnnouncementPlatform.IOS ? stores.ios() : stores.android();
        return AppAnnouncementUrls.httpsOrNull(store);
    }

    private static boolean bothColumnsEmpty(AppAnnouncementCandidate candidate) {
        return !isSet(candidate.ctaUrlIos()) && !isSet(candidate.ctaUrlAndroid());
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static int typeRank(AppAnnouncementType type) {
        if (type == AppAnnouncementType.MAINTENANCE) {
            return 0;
        }
        if (type == AppAnnouncementType.UPDATE) {
            return 1;
        }
        return 2;
    }

    private static final Comparator<Match> ORDER = Comparator
            .comparing((Match match) -> match.candidate().dismissible())
            .thenComparingInt(match -> typeRank(match.candidate().type()))
            .thenComparing(Comparator.comparingInt((Match match) -> match.candidate().priority()).reversed())
            .thenComparing(Comparator.comparing(
                    (Match match) -> match.candidate().createdAt() == null ? Instant.EPOCH : match.candidate().createdAt()
            ).reversed())
            .thenComparing(Comparator.comparingLong((Match match) -> match.candidate().id()).reversed());

    private record Match(AppAnnouncementCandidate candidate, AppAnnouncementCandidate.Text text, ResolvedCtas ctas) {
    }

    private record ResolvedCtas(String selected, String ios, String android) {
    }
}
