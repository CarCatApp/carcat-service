package com.carland.carland_service.entity;

import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Bir version + named flag + rol için görünürlük.
 * en: Visibility for one version + named flag + role.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "feature_flag_role_state",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ffrs_flag_version_role",
                columnNames = {"flag_id", "version_id", "role"}
        )
)
public class FeatureFlagRoleState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flag_id")
    FeatureFlag flag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    AppVersion version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    UserRoles role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    FeatureFlagState state;
}
