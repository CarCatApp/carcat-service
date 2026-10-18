package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * Brand name a branch shows to customers under a shared heading.
 * Series, viscosity and unit stay on {@link BrandModel}.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "branch_serviced_goods",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_branch_serviced_good_name",
                columnNames = {"branch_id", "brand_model_service_id", "name"}
        )
)
public class BranchServicedGood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_model_service_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BrandModelService brandModelService;

    @Column(nullable = false, length = 80)
    String name;

    /**
     * tr: Vahid qiyməti, AZN. Xidməti tamamla zamanı miqdarla vurulur. Boş ola bilər.
     * en: Unit price in AZN. Multiplied by quantity on service completion. Optional.
     */
    @Column(precision = 10, scale = 2)
    java.math.BigDecimal price;
}
