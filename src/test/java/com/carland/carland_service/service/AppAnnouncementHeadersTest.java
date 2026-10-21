package com.carland.carland_service.service;

import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.util.AppVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppAnnouncementHeadersTest {

    @Test
    void languageTakesPrimaryTag() {
        assertEquals("az", AppAnnouncementHeaders.language(null));
        assertEquals("az", AppAnnouncementHeaders.language(""));
        assertEquals("az", AppAnnouncementHeaders.language("az-AZ,az;q=0.9"));
        assertEquals("ru", AppAnnouncementHeaders.language("ru;q=0.8"));
        assertEquals("en", AppAnnouncementHeaders.language("EN-us"));
        assertEquals("az", AppAnnouncementHeaders.language("tr"));
        assertEquals("az", AppAnnouncementHeaders.language("x".repeat(129)));
    }

    @Test
    void platformIsCaseInsensitive() {
        assertEquals(AppAnnouncementPlatform.ANDROID, AppAnnouncementHeaders.platform("android"));
        assertEquals(AppAnnouncementPlatform.IOS, AppAnnouncementHeaders.platform(" IOS "));
        assertNull(AppAnnouncementHeaders.platform("WINDOWS"));
        assertNull(AppAnnouncementHeaders.platform(null));
        assertNull(AppAnnouncementHeaders.platform(""));
        assertNull(AppAnnouncementHeaders.platform("A".repeat(33)));
    }

    @Test
    void versionUsesTolerantParser() {
        assertEquals(AppVersion.parse("3.0.5").orElseThrow(), AppAnnouncementHeaders.version("3.0.5+76"));
        assertEquals(AppVersion.parse("3.0.5").orElseThrow(), AppAnnouncementHeaders.version(" 3.0.5-beta "));
        assertNull(AppAnnouncementHeaders.version("garbage"));
        assertNull(AppAnnouncementHeaders.version(null));
        assertNull(AppAnnouncementHeaders.version(""));
        assertNull(AppAnnouncementHeaders.version("1".repeat(33)));
    }
}
