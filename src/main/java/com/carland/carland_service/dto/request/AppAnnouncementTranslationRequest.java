package com.carland.carland_service.dto.request;

import lombok.Data;

@Data
public class AppAnnouncementTranslationRequest {
    private String lang;
    private String title;
    private String body;
    private String ctaLabel;
}
