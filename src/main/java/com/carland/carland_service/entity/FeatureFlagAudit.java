package com.carland.carland_service.entity;

import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * tr: Admin panel feature-flag değişikliği (kim, ne zaman, hangi endpoint/rol, eski→yeni).
 * en: Admin-panel feature-flag change (who, when, which endpoint/role, old→new).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "feature_flag_audit")
public class FeatureFlagAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(nullable = false, length = 64)
    String actor;

    @Column(name = "http_method", nullable = false, length = 8)
    String httpMethod;

    @Column(name = "path_pattern", nullable = false, length = 256)
    String pathPattern;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    UserRoles role;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_state", length = 16)
    FeatureFlagState oldState;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_state", nullable = false, length = 16)
    FeatureFlagState newState;

    @Column(name = "app_version", nullable = false, length = 32)
    String appVersion;
}
