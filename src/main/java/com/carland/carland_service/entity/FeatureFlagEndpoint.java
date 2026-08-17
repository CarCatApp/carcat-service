package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Guard edilebilen Spring path (method + path_pattern); version'dan bağımsız.
 * en: Guardable Spring path (method + path_pattern); independent of app version.
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
}
