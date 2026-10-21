package com.carland.carland_service.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * tr: CTA adresleri yalnız https. Admin yazımında host allowlist'te olmalı.
 * en: CTA URLs are https only. Admin writes also require an allowlisted host.
 */
public final class AppAnnouncementUrls {

    private AppAnnouncementUrls() {
    }

    public static String httpsOrNull(String raw) {
        URI uri = parseHttps(raw);
        if (uri == null) {
            return null;
        }
        return raw.trim();
    }

    public static String allowlistedOrNull(String raw, Collection<String> hosts) {
        URI uri = parseHttps(raw);
        if (uri == null || hosts == null) {
            return null;
        }
        String host = uri.getHost();
        for (String allowed : hosts) {
            if (allowed != null && allowed.trim().equalsIgnoreCase(host)) {
                return raw.trim();
            }
        }
        return null;
    }

    private static URI parseHttps(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(raw.trim());
            if (uri.getUserInfo() != null) {
                return null;
            }
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return null;
            }
            return uri;
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}
