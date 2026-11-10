package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    String titleEn;
    String bodyEn;
    String titleRu;
    String bodyRu;
    String status;
    int audienceCount;
    int successCount;
    int failedCount;
    String gender;
    String brand;
    Long engineTypeId;
    String engineTypeName;
    List<String> phoneNumbers;
    String createdBy;
    String errorMessage;
    LocalDateTime createdAt;
    LocalDateTime startedAt;
    LocalDateTime finishedAt;
}
