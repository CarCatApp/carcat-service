package com.carland.carland_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: Mağaza linkleri ve CTA host allowlist'i. Kodda sabitlenmez; iOS linki env dolmadan boştur.
 * en: Store links and the CTA host allowlist. Not hardcoded; the iOS link stays empty until env is set.
 */
@Data
@ConfigurationProperties(prefix = "carland.app-announcements")
public class AppAnnouncementProperties {

    private int cacheTtlSeconds = 60;

    private String androidStoreUrl = "https://play.google.com/store/apps/details?id=com.nematmirzayev.carcat";

    private String iosStoreUrl = "";

    private List<String> ctaHostAllowlist = new ArrayList<>(List.of(
            "play.google.com",
            "apps.apple.com",
            "itunes.apple.com"
    ));
}
