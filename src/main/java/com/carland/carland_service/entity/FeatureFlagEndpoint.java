package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Guard edilebilir Spring path; flag_id doluysa bir feature flag'e aittir (claimed).
 * en: Guardable Spring path; non-null flag_id means claimed by a feature flag.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "feature_flag_endpoint",
        uniqueConstraints = @UniqueConstraint(name = "uk_ffe_method_path", columnNames = {"http_method", "path_pattern"})
)
public class FeatureFlagEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "http_method", nullable = false, length = 8)
    String httpMethod;

    @Column(name = "path_pattern", nullable = false, length = 256)
    String pathPattern;

    @Column(name = "never_guard", nullable = false)
    boolean neverGuard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flag_id")
    FeatureFlag flag;

    public boolean isClaimed() {
        return flag != null;
    }
}
