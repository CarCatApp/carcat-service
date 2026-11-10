package com.carland.carland_service.service;

import com.carland.carland_service.entity.PushCampaign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminPushCampaignServiceTest {

    @Test
    void picksLanguageWithAzFallback() {
        PushCampaign campaign = PushCampaign.builder()
                .title("Az title")
                .body("Az body")
                .titleEn("En title")
                .bodyEn("En body")
                .titleRu("Ru title")
                .build();

        assertEquals("En title", AdminPushCampaignService.titleFor(campaign, "en"));
        assertEquals("En body", AdminPushCampaignService.bodyFor(campaign, "en"));
        assertEquals("Ru title", AdminPushCampaignService.titleFor(campaign, "ru"));
        assertEquals("Az body", AdminPushCampaignService.bodyFor(campaign, "ru"));
        assertEquals("Az title", AdminPushCampaignService.titleFor(campaign, "az"));
        assertEquals("Az title", AdminPushCampaignService.titleFor(campaign, null));
    }
}
