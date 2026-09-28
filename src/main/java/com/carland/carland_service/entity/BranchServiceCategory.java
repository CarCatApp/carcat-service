package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Bir şubenin bir kategoriyi açıp kapatması. Satır yoksa toggleable kategori kapalı sayılır.
 * en: Per-branch on/off for a category. No row means a toggleable category is off.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "branch_service_categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_branch_service_categories_branch_category",
                columnNames = {"branch_id", "category_id"}
        )
)
public class BranchServiceCategory {

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
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    ServiceCategory category;

    @Column(nullable = false)
    Boolean active;
}
