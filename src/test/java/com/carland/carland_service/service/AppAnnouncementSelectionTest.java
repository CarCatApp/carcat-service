package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.AppAnnouncementActiveItem;
import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.enums.AppAnnouncementType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppAnnouncementSelectionTest {

    private static final Instant T = Instant.parse("2026-06-01T12:00:00Z");
    private static final String IOS_STORE = "https://apps.apple.com/app/id000";
    private static final String ANDROID_STORE = "https://play.google.com/store/apps/details?id=com.nematmirzayev.carcat";
    private static final StoreLinks STORES = new StoreLinks(IOS_STORE, ANDROID_STORE);
    private static final Set<String> HOSTS = Set.of("play.google.com", "apps.apple.com", "itunes.apple.com");

    @Test
    void scenario1_iosAtMaxGetsForcedUpdateWithIosStore() {
        List<AppAnnouncementActiveItem> items = select(sample(), "az", "3.0.5", "IOS");
        assertEquals(List.of(1L), ids(items));
        assertEquals("UPDATE", items.get(0).getType());
        assertFalse(items.get(0).isDismissible());
        assertEquals("A1-AZ", items.get(0).getTitle());
        assertEquals(IOS_STORE, items.get(0).getCtaUrl());
    }

    @Test
    void scenario2_newerAndroidFallsBackToAz() {
        List<AppAnnouncementActiveItem> items = select(sample(), "ru", "3.0.10", "ANDROID");
        assertEquals(List.of(2L), ids(items));
        assertEquals("A2-AZ", items.get(0).getTitle());
        assertEquals("A2 body", items.get(0).getBody());
    }

    @Test
    void scenario3_minInclusiveAndNewerFirst() {
        List<AppAnnouncementActiveItem> items = select(sample(), "en", "3.1.0", "ANDROID");
        assertEquals(List.of(3L, 2L), ids(items));
        assertEquals("A3-AZ", items.get(0).getTitle());
        assertEquals("A2-AZ", items.get(1).getTitle());
    }

    @Test
    void scenario4_noHeadersIsEmpty() {
        assertTrue(select(sample(), null, null, null).isEmpty());
    }

    @Test
    void scenario5_garbageVersionIsEmpty() {
        assertTrue(select(sample(), null, "garbage", "IOS").isEmpty());
    }

    @Test
    void scenario7_lowercasePlatformMatchesAndroid() {
        assertEquals(AppAnnouncementPlatform.ANDROID, AppAnnouncementHeaders.platform("android"));
        assertEquals(ids(select(sample(), "ru", "3.0.10", "ANDROID")),
                ids(select(sample(), "ru", "3.0.10", "android")));
    }

    @Test
    void scenario8_unknownPlatformDropsUpdateWithoutCta() {
        assertTrue(select(sample(), "az", "3.0.5", "WINDOWS").isEmpty());
        List<AppAnnouncementCandidate> withLink = new ArrayList<>(sample());
        withLink.set(0, copyWithCta(sample().get(0), ANDROID_STORE));
        List<AppAnnouncementActiveItem> items = select(withLink, "az", "3.0.5", "WINDOWS");
        assertEquals(List.of(1L), ids(items));
        assertEquals(ANDROID_STORE, items.get(0).getCtaUrl());
    }

    @Test
    void scenario9_startsAtIsInclusive() {
        AppAnnouncementCandidate row = info(10, AppAnnouncementPlatform.ALL, null, null, T, null, T, true, text("az", "Now", "Body"));
        assertEquals(List.of(10L), ids(select(List.of(row), "az", null, null)));
    }

    @Test
    void scenario10_endsAtIsExclusive() {
        AppAnnouncementCandidate row = info(11, AppAnnouncementPlatform.ALL, null, null, null, T, T, true, text("az", "End", "Body"));
        assertTrue(select(List.of(row), "az", null, null).isEmpty());
    }

    @Test
    void scenario11_updateWithoutCtaIsDropped() {
        AppAnnouncementCandidate row = base(12, AppAnnouncementType.UPDATE, false, AppAnnouncementPlatform.ALL,
                null, "3.0.5", null, null, T, true, null, 0, text("az", "Up", "Body"));
        assertTrue(AppAnnouncementSelection.select(
                List.of(row),
                AppAnnouncementHeaders.resolve("az", "3.0.5", "IOS"),
                T,
                new StoreLinks("", ""),
                HOSTS
        ).isEmpty());
    }

    @Test
    void scenario12_blankTitleFallsBackToAzThenDrops() {
        AppAnnouncementCandidate fallback = base(13, AppAnnouncementType.INFO, true, AppAnnouncementPlatform.ALL,
                null, null, null, null, T, true, null, 0,
                text("en", "   ", "ignored"),
                text("az", "AZ-TITLE", "AZ-BODY"));
        List<AppAnnouncementActiveItem> items = select(List.of(fallback), "en", null, null);
        assertEquals("AZ-TITLE", items.get(0).getTitle());

        AppAnnouncementCandidate emptyAz = base(14, AppAnnouncementType.INFO, true, AppAnnouncementPlatform.ALL,
                null, null, null, null, T, true, null, 0,
                text("en", "   ", "x"),
                text("az", "  ", "  "));
        assertTrue(select(List.of(emptyAz), "en", null, null).isEmpty());
    }

    @Test
    void scenario13_limitFiveKeepsSortOrder() {
        Instant created = T;
        List<AppAnnouncementCandidate> rows = List.of(
                base(1, AppAnnouncementType.INFO, true, AppAnnouncementPlatform.ALL, null, null, null, null, created, true, null, 0, text("az", "1", "b")),
                base(2, AppAnnouncementType.INFO, true, AppAnnouncementPlatform.ALL, null, null, null, null, created, true, null, 10, text("az", "2", "b")),
                base(3, AppAnnouncementType.UPDATE, true, AppAnnouncementPlatform.ALL, null, "9.0.0", null, null, created, true, ANDROID_STORE, 0, text("az", "3", "b")),
                base(4, AppAnnouncementType.MAINTENANCE, true, AppAnnouncementPlatform.ALL, null, null, null, null, created, true, null, 0, text("az", "4", "b")),
                base(5, AppAnnouncementType.INFO, false, AppAnnouncementPlatform.ALL, null, null, null, null, created, true, null, 0, text("az", "5", "b")),
                base(6, AppAnnouncementType.UPDATE, false, AppAnnouncementPlatform.ALL, null, "9.0.0", null, null, created, true, ANDROID_STORE, 0, text("az", "6", "b"))
        );
        assertEquals(List.of(6L, 5L, 4L, 3L, 2L), ids(select(rows, "az", "1.0.0", "IOS")));
    }

    @Test
    void scenario14And15And16_jsonShapeIdAndAzerbaijani() throws Exception {
        AppAnnouncementCandidate row = base(16, AppAnnouncementType.INFO, true, AppAnnouncementPlatform.ALL,
                null, null, null, null, T, true, null, 0,
                new AppAnnouncementCandidate.Text("az", "Şəhər ğı ö ü ç ə ı", "Mətn\nsətir", null));
        AppAnnouncementActiveItem item = select(List.of(row), "az", null, null).get(0);
        assertEquals("Şəhər ğı ö ü ç ə ı", item.getTitle());
        assertNull(item.getCtaLabel());
        assertNull(item.getCtaUrl());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(mapper.writeValueAsString(item));
        assertEquals(Set.of("id", "type", "dismissible", "title", "body", "ctaLabel", "ctaUrl"),
                Set.copyOf(iterable(node.fieldNames())));
        assertTrue(node.get("id").isNumber());
        assertFalse(node.get("id").isTextual());
        assertEquals(16, node.get("id").asInt());
        assertTrue(node.get("title").asText().contains("Şəhər"));
        assertFalse(node.has("platform"));
        assertFalse(node.has("minAppVersion"));
        assertFalse(node.has("priority"));
        assertFalse(node.has("active"));
    }

    @Test
    void candidateCacheRoundTripKeepsInstants() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        List<AppAnnouncementCandidate> rows = sample();
        List<AppAnnouncementCandidate> restored = mapper.readValue(
                mapper.writeValueAsString(rows),
                new TypeReference<>() {
                }
        );
        assertEquals(rows, restored);
        assertEquals(ids(select(restored, "az", "3.0.5", "IOS")), ids(select(rows, "az", "3.0.5", "IOS")));
    }

    private static List<AppAnnouncementActiveItem> select(List<AppAnnouncementCandidate> rows, String lang, String version, String platform) {
        return AppAnnouncementSelection.select(
                rows,
                AppAnnouncementHeaders.resolve(lang, version, platform),
                T,
                STORES,
                HOSTS
        );
    }

    private static List<Long> ids(List<AppAnnouncementActiveItem> items) {
        return items.stream().map(AppAnnouncementActiveItem::getId).toList();
    }

    private static List<AppAnnouncementCandidate> sample() {
        return List.of(
                base(1, AppAnnouncementType.UPDATE, false, AppAnnouncementPlatform.ALL,
                        null, "3.0.5", null, null, T.minus(10, ChronoUnit.DAYS), true, null, 0,
                        text("az", "A1-AZ", "Yenilə"),
                        text("en", "A1-EN", "Update")),
                base(2, AppAnnouncementType.INFO, true, AppAnnouncementPlatform.ANDROID,
                        null, null, T.minus(1, ChronoUnit.DAYS), T.plus(1, ChronoUnit.DAYS),
                        T.minus(2, ChronoUnit.DAYS), true, null, 0,
                        text("az", "A2-AZ", "A2 body")),
                base(3, AppAnnouncementType.INFO, true, AppAnnouncementPlatform.ALL,
                        "3.1.0", null, null, null, T.minus(1, ChronoUnit.DAYS), true, null, 0,
                        text("az", "A3-AZ", "A3 body")),
                base(4, AppAnnouncementType.INFO, true, AppAnnouncementPlatform.ALL,
                        null, null, null, T.minusSeconds(1), T, true, null, 0,
                        text("az", "A4", "expired")),
                base(5, AppAnnouncementType.INFO, true, AppAnnouncementPlatform.ALL,
                        null, null, null, null, T, false, null, 0,
                        text("az", "A5", "inactive"))
        );
    }

    private static AppAnnouncementCandidate info(
            long id,
            AppAnnouncementPlatform platform,
            String min,
            String max,
            Instant starts,
            Instant ends,
            Instant created,
            boolean active,
            AppAnnouncementCandidate.Text... texts
    ) {
        return base(id, AppAnnouncementType.INFO, true, platform, min, max, starts, ends, created, active, null, 0, texts);
    }

    private static AppAnnouncementCandidate copyWithCta(AppAnnouncementCandidate source, String ctaUrl) {
        return new AppAnnouncementCandidate(
                source.id(), source.type(), source.dismissible(), source.platform(),
                source.minAppVersion(), source.maxAppVersion(), ctaUrl, source.priority(),
                source.startsAt(), source.endsAt(), source.createdAt(), source.active(), source.translations()
        );
    }

    private static AppAnnouncementCandidate.Text text(String lang, String title, String body) {
        return new AppAnnouncementCandidate.Text(lang, title, body, null);
    }

    private static AppAnnouncementCandidate base(
            long id,
            AppAnnouncementType type,
            boolean dismissible,
            AppAnnouncementPlatform platform,
            String min,
            String max,
            Instant starts,
            Instant ends,
            Instant created,
            boolean active,
            String ctaUrl,
            int priority,
            AppAnnouncementCandidate.Text... texts
    ) {
        return new AppAnnouncementCandidate(
                id, type, dismissible, platform, min, max, ctaUrl, priority, starts, ends, created, active, List.of(texts)
        );
    }

    private static <T> List<T> iterable(java.util.Iterator<T> iterator) {
        List<T> values = new ArrayList<>();
        iterator.forEachRemaining(values::add);
        return values;
    }
}
