package com.carland.carland_service.entity;

import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.enums.AppAnnouncementType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

/**
 * tr: Mobil açılış duyurusu. {@code active=false} taslaktır; süre penceresi okuma anında süzülür.
 * en: Cold-start announcement. {@code active=false} is a draft; the time window is applied on read.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@Table(name = "app_announcement")
public class AppAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    AppAnnouncementType type;

    @Builder.Default
    @Column(nullable = false)
    boolean dismissible = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    AppAnnouncementPlatform platform = AppAnnouncementPlatform.ALL;

    @Column(name = "min_app_version", length = 32)
    String minAppVersion;

    @Column(name = "max_app_version", length = 32)
    String maxAppVersion;

    @Column(name = "cta_url", length = 512)
    String ctaUrl;

    @Builder.Default
    @Column(nullable = false)
    int priority = 0;

    @Column(name = "starts_at")
    Instant startsAt;

    @Column(name = "ends_at")
    Instant endsAt;

    @Builder.Default
    @Column(nullable = false)
    boolean active = false;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at")
    Instant updatedAt;

    @Column(name = "created_by", length = 64)
    String createdBy;

    @Column(name = "updated_by", length = 64)
    String updatedBy;

    @Builder.Default
    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    List<AppAnnouncementTranslation> translations = new ArrayList<>();
}
