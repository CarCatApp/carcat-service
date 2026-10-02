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
 * tr: Şubenin fərdi xidmət satırı. Fiyatlar deaktiv olunca da kalır.
 * en: A branch's individual-service row. Prices stay when the row is turned off.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "branch_individual_services",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_branch_individual_service",
                columnNames = {"branch_id", "individual_service_id"}
        )
)
public class BranchIndividualService {

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
    @JoinColumn(name = "individual_service_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    IndividualService individualService;

    /** Branch offer. False keeps the prices and hides the line from the customer. */
    @Column(nullable = false)
    @Builder.Default
    Boolean active = false;

    /** Whole manat. Null means the branch has not set this tier. */
    @Column(name = "price_simple")
    Integer priceSimple;

    @Column(name = "price_medium")
    Integer priceMedium;

    @Column(name = "price_complex")
    Integer priceComplex;
}
