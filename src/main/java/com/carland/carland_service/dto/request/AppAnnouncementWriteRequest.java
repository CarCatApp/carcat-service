package com.carland.carland_service.dto.request;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class AppAnnouncementWriteRequest {
    private String type;
    private Boolean dismissible;
    private String platform;
    private String minAppVersion;
    private String maxAppVersion;
    private String ctaUrl;
    private Integer priority;
    private Instant startsAt;
    private Instant endsAt;
    private List<AppAnnouncementTranslationRequest> translations;
}
