package com.carland.carland_service.entity;

import com.carland.carland_service.enums.FeatureFlagState;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Ürün feature flag'i (isimli); API'ler bu kayda attach edilir.
 * en: Named product feature flag; APIs attach to this row.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "feature_flag")
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 128)
    String name;

    @Column(length = 512)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_state", nullable = false, length = 16)
    FeatureFlagState defaultState;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "flag")
    @Builder.Default
    @ToString.Exclude
    List<FeatureFlagEndpoint> endpoints = new ArrayList<>();
}
