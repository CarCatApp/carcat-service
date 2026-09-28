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
 * tr: Pakete eklenen xidmət. enabled false satırı silmez, sadece paketten çıkarır.
 * en: A service line on a package. enabled false keeps the row and drops it from the package.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "branch_care_package_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_branch_care_package_items_pkg_svc",
                columnNames = {"package_id", "offered_service_id"}
        )
)
public class BranchCarePackageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BranchCarePackage carePackage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offered_service_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    OfferedService offeredService;

    @Column(nullable = false)
    @Builder.Default
    Boolean enabled = true;
}
