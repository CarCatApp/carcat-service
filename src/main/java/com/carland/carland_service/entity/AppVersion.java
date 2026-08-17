package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * tr: Mobil app semver kaydı; feature-flag grid'i version snapshot'larına bağlanır.
 * en: Mobile app semver row; feature-flag grids bind to version snapshots.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "app_version")
public class AppVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 32)
    String semver;

    @Column(name = "is_current", nullable = false)
    boolean current;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}
