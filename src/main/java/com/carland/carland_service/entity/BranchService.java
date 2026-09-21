package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Şube hizmeti (svc:…) veya yön (dir:repair|inspection|trade). Fiyat qəpik.
 * en: Branch service (svc:…) or direction (dir:repair|inspection|trade). Prices in qepik.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "branch_services",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_branch_services_branch_key",
                columnNames = {"branch_id", "service_key"}
        )
)
public class BranchService {

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

    /** SERVICE or DIRECTION */
    @Column(nullable = false, length = 16)
    @Builder.Default
    String kind = "SERVICE";

    @Column(name = "title_json", nullable = false, length = 1024)
    String titleJson;

    @Column(name = "price_min")
    Integer priceMin;

    @Column(name = "price_max")
    Integer priceMax;

    @Column(length = 8)
    @Builder.Default
    String currency = "AZN";

    @Column(name = "duration_min")
    Integer durationMin;

    @Column(nullable = false)
    @Builder.Default
    Boolean active = true;
}
