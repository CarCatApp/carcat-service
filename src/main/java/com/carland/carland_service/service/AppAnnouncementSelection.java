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
            String ctaUrl = resolveCta(candidate, ctx.platform(), stores, ctaHosts);
            if (candidate.type() == AppAnnouncementType.UPDATE && ctaUrl == null) {
                continue;
            }
            matches.add(new Match(candidate, text, ctaUrl));
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
                    .ctaUrl(match.ctaUrl())
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

    private static String resolveCta(
            AppAnnouncementCandidate candidate,
            AppAnnouncementPlatform requestPlatform,
            StoreLinks stores,
            Collection<String> ctaHosts
    ) {
        String explicit = AppAnnouncementUrls.allowlistedOrNull(platformCta(candidate, requestPlatform), ctaHosts);
        if (explicit == null) {
            explicit = AppAnnouncementUrls.allowlistedOrNull(candidate.ctaUrl(), ctaHosts);
        }
        if (explicit != null) {
            return explicit;
        }
        if (candidate.type() != AppAnnouncementType.UPDATE || requestPlatform == null || stores == null) {
            return null;
        }
        String store = requestPlatform == AppAnnouncementPlatform.IOS ? stores.ios() : stores.android();
        return AppAnnouncementUrls.httpsOrNull(store);
    }

    private static String platformCta(AppAnnouncementCandidate candidate, AppAnnouncementPlatform requestPlatform) {
        if (requestPlatform == AppAnnouncementPlatform.IOS) {
            return candidate.ctaUrlIos();
        }
        if (requestPlatform == AppAnnouncementPlatform.ANDROID) {
            return candidate.ctaUrlAndroid();
        }
        return null;
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

    private record Match(AppAnnouncementCandidate candidate, AppAnnouncementCandidate.Text text, String ctaUrl) {
    }
}
