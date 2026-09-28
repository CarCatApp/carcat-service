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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * tr: Şubenin kendi dövri qulluq paketi. Müşteri kataloğu branch_packages değildir.
 * en: A branch's own routine-care package. This is not the customer-catalog branch_packages row.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "branch_care_packages")
public class BranchCarePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Branch branch;

    @Column(nullable = false, length = 80)
    String name;

    /** Whole manat, the number typed on the create form. */
    @Column(nullable = false)
    Integer price;

    @Column(nullable = false, length = 8)
    @Builder.Default
    String currency = "AZN";

    @Column(nullable = false)
    @Builder.Default
    Boolean active = true;
}
