package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AppAnnouncementTranslationRequest;
import com.carland.carland_service.dto.request.AppAnnouncementWriteRequest;
import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.enums.AppAnnouncementType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppAnnouncementRulesTest {

    private static final StoreLinks BOTH = new StoreLinks(
            "https://apps.apple.com/app/id1",
            "https://play.google.com/store/apps/details?id=com.nematmirzayev.carcat"
    );
    private static final StoreLinks ANDROID_ONLY = new StoreLinks(
            "",
            "https://play.google.com/store/apps/details?id=com.nematmirzayev.carcat"
    );
    private static final Set<String> HOSTS = Set.of("play.google.com", "apps.apple.com", "itunes.apple.com");

    @Test
    void infoDefaults() {
        AppAnnouncementRules.Normalized normalized = AppAnnouncementRules.validate(info("Salam", "Mətn"), BOTH, HOSTS);
        assertEquals(AppAnnouncementType.INFO, normalized.type());
        assertTrue(normalized.dismissible());
        assertEquals(AppAnnouncementPlatform.ALL, normalized.platform());
        assertEquals(0, normalized.priority());
        assertNull(normalized.ctaUrl());
        assertNull(normalized.ctaUrlIos());
        assertNull(normalized.ctaUrlAndroid());
        assertEquals("az", normalized.translations().get(0).lang());
    }

    @Test
    void azIsRequired() {
        AppAnnouncementWriteRequest request = info("Hello", "Body");
        request.getTranslations().get(0).setLang("en");
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(request, BOTH, HOSTS));
    }

    @Test
    void lengthsAreCapped() {
        AppAnnouncementWriteRequest title = info("x".repeat(61), "body");
        AppAnnouncementWriteRequest body = info("title", "y".repeat(501));
        AppAnnouncementWriteRequest label = info("title", "body");
        label.getTranslations().get(0).setCtaLabel("z".repeat(31));
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(title, BOTH, HOSTS));
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(body, BOTH, HOSTS));
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(label, BOTH, HOSTS));
    }

    @Test
    void updateRequiresMaxAndOrderedBounds() {
        AppAnnouncementWriteRequest missingMax = info("title", "body");
        missingMax.setType("UPDATE");
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(missingMax, BOTH, HOSTS));

        AppAnnouncementWriteRequest inverted = info("title", "body");
        inverted.setType("UPDATE");
        inverted.setMinAppVersion("3.0.10");
        inverted.setMaxAppVersion("3.0.5");
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(inverted, BOTH, HOSTS));

        AppAnnouncementWriteRequest junk = info("title", "body");
        junk.setMaxAppVersion("3.x.1");
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(junk, BOTH, HOSTS));
    }

    @Test
    void endsAtMustBeAfterStartsAt() {
        Instant start = Instant.parse("2026-06-01T12:00:00Z");
        AppAnnouncementWriteRequest same = info("title", "body");
        same.setStartsAt(start);
        same.setEndsAt(start);
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(same, BOTH, HOSTS));
    }

    @Test
    void ctaHostMustBeAllowlistedHttps() {
        AppAnnouncementWriteRequest http = info("title", "body");
        http.setCtaUrl("http://play.google.com/store");
        AppAnnouncementWriteRequest foreign = info("title", "body");
        foreign.setCtaUrl("https://evil.example/phish");
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(http, BOTH, HOSTS));
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(foreign, BOTH, HOSTS));

        AppAnnouncementWriteRequest ok = info("title", "body");
        ok.setCtaUrl("https://play.google.com/store/apps/details?id=com.nematmirzayev.carcat");
        assertEquals(ok.getCtaUrl(), AppAnnouncementRules.validate(ok, BOTH, HOSTS).ctaUrl());
    }

    @Test
    void forcedUpdateNeedsResolvableCta() {
        AppAnnouncementWriteRequest allPlatforms = forcedUpdate(null);
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(allPlatforms, ANDROID_ONLY, HOSTS));

        AppAnnouncementWriteRequest ios = forcedUpdate(null);
        ios.setPlatform("IOS");
        assertThrows(IllegalArgumentException.class, () -> AppAnnouncementRules.validate(ios, ANDROID_ONLY, HOSTS));

        AppAnnouncementWriteRequest android = forcedUpdate(null);
        android.setPlatform("ANDROID");
        AppAnnouncementRules.Normalized normalized = AppAnnouncementRules.validate(android, ANDROID_ONLY, HOSTS);
        assertEquals(AppAnnouncementPlatform.ANDROID, normalized.platform());

        AppAnnouncementWriteRequest withLink = forcedUpdate(
                "https://apps.apple.com/app/id1");
        AppAnnouncementRules.validate(withLink, ANDROID_ONLY, HOSTS);
    }

    @Test
    void allPlatformKeepsSeparateStoreLinks() {
        String ios = "https://apps.apple.com/app/id1";
        String android = "https://play.google.com/store/apps/details?id=com.nematmirzayev.carcat";
        AppAnnouncementWriteRequest request = forcedUpdate(null);
        request.setCtaUrlIos(ios);
        request.setCtaUrlAndroid(android);
        AppAnnouncementRules.Normalized normalized = AppAnnouncementRules.validate(request, new StoreLinks("", ""), HOSTS);
        assertNull(normalized.ctaUrl());
        assertEquals(ios, normalized.ctaUrlIos());
        assertEquals(android, normalized.ctaUrlAndroid());
    }

    @Test
    void allForcedUpdateNeedsAndroidWhenOnlyIosLinkIsSet() {
        AppAnnouncementWriteRequest request = forcedUpdate(null);
        request.setCtaUrlIos("https://apps.apple.com/app/id1");
        assertThrows(IllegalArgumentException.class,
                () -> AppAnnouncementRules.validate(request, new StoreLinks("", ""), HOSTS));
    }

    @Test
    void singlePlatformDropsTheOtherLink() {
        AppAnnouncementWriteRequest request = forcedUpdate("https://apps.apple.com/app/id9");
        request.setPlatform("IOS");
        request.setCtaUrlAndroid("https://play.google.com/store/apps/details?id=com.nematmirzayev.carcat");
        AppAnnouncementRules.Normalized normalized = AppAnnouncementRules.validate(request, new StoreLinks("", ""), HOSTS);
        assertEquals("https://apps.apple.com/app/id9", normalized.ctaUrl());
        assertNull(normalized.ctaUrlIos());
        assertNull(normalized.ctaUrlAndroid());
    }

    private static AppAnnouncementWriteRequest info(String title, String body) {
        AppAnnouncementTranslationRequest translation = new AppAnnouncementTranslationRequest();
        translation.setLang("az");
        translation.setTitle(title);
        translation.setBody(body);
        AppAnnouncementWriteRequest request = new AppAnnouncementWriteRequest();
        request.setType("INFO");
        request.setTranslations(List.of(translation));
        return request;
    }

    private static AppAnnouncementWriteRequest forcedUpdate(String ctaUrl) {
        AppAnnouncementWriteRequest request = info("Yenilə", "Tətbiqi yeniləyin");
        request.setType("UPDATE");
        request.setDismissible(false);
        request.setPlatform("ALL");
        request.setMaxAppVersion("3.0.5");
        request.setCtaUrl(ctaUrl);
        return request;
    }
}
