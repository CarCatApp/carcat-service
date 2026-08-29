package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * tr: Admin push kampanyasının kuyruk / ilerleme görünümü.
 * en: Admin push campaign queue / progress view.
 */
@Data
@Builder
public class AdminPushCampaignView {
    Long id;
    String title;
    String body;
    String status;
    int audienceCount;
    int successCount;
    int failedCount;
    String gender;
    String brand;
    Long engineTypeId;
    String engineTypeName;
    String createdBy;
    String errorMessage;
    LocalDateTime createdAt;
    LocalDateTime startedAt;
    LocalDateTime finishedAt;
}
