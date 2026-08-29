package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * tr: Admin toplu push kampanyası; kuyrukta durur, arka planda FCM multicast ile işlenir.
 * en: Admin bulk push campaign; queued then processed in the background via FCM multicast.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "push_campaigns")
public class PushCampaign {

    public static final String QUEUED = "QUEUED";
    public static final String SENDING = "SENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 100)
    String title;

    @Column(nullable = false, length = 300)
    String body;

    @Column(name = "filter_gender", length = 16)
    String filterGender;

    @Column(name = "filter_brand", length = 64)
    String filterBrand;

    @Column(name = "filter_engine_type_id")
    Long filterEngineTypeId;

    @Column(name = "filter_engine_type_name", length = 64)
    String filterEngineTypeName;

    @Column(nullable = false, length = 16)
    String status;

    @Column(name = "audience_count", nullable = false)
    @Builder.Default
    Integer audienceCount = 0;

    @Column(name = "success_count", nullable = false)
    @Builder.Default
    Integer successCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    Integer failedCount = 0;

    @Column(name = "created_by", length = 64)
    String createdBy;

    @Column(name = "error_message", length = 512)
    String errorMessage;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "started_at")
    LocalDateTime startedAt;

    @Column(name = "finished_at")
    LocalDateTime finishedAt;
}
