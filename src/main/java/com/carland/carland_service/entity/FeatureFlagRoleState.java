package com.carland.carland_service.entity;

import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Named flag + rol için görünürlük (ENABLED / DISABLED / HIDDEN).
 * en: Visibility for one named flag + role (ENABLED / DISABLED / HIDDEN).
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
                name = "uk_ffrs_flag_role",
                columnNames = {"flag_id", "role"}
        )
)
public class FeatureFlagRoleState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flag_id")
    FeatureFlag flag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    UserRoles role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    FeatureFlagState state;
}
