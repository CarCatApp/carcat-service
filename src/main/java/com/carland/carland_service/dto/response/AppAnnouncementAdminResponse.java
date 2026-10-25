package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppAnnouncementAdminResponse {
    private long id;
    private String type;
    private boolean dismissible;
    private String platform;
    private String minAppVersion;
    private String maxAppVersion;
    private String ctaUrl;
    private String ctaUrlIos;
    private String ctaUrlAndroid;
    private int priority;
    private Instant startsAt;
    private Instant endsAt;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private List<Translation> translations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Translation {
        private String lang;
        private String title;
        private String body;
        private String ctaLabel;
    }
}
