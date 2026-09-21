package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Şubeye ait paket (pkg:…). Fiyat qəpik.
 * en: Branch package (pkg:…). Prices in qepik.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "branch_packages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_branch_packages_branch_key",
                columnNames = {"branch_id", "service_key"}
        )
)
public class BranchPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Branch branch;

    @Column(name = "service_key", nullable = false, length = 64)
    String serviceKey;

    @Column(name = "title_json", nullable = false, length = 1024)
    String titleJson;

    @Column(name = "price_min", nullable = false)
    Integer priceMin;

    @Column(name = "price_max", nullable = false)
    Integer priceMax;

    @Column(nullable = false, length = 8)
    @Builder.Default
    String currency = "AZN";

    @Column(name = "duration_min")
    Integer durationMin;

    @Column(name = "included_service_keys", length = 2000)
    String includedServiceKeys;

    @Column(nullable = false)
    @Builder.Default
    Boolean active = true;
}
