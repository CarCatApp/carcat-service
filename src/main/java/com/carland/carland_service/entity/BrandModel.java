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
 * tr: Bir başlığın altında, bir şubenin markası. Seriya her satırda durur; özüllük yalnız yağ başlığında dolar.
 * en: A branch brand under one shared heading. Series is always stored; viscosity is filled only on oil headings.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "brand_models")
public class BrandModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_model_service_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BrandModelService brandModelService;

    /**
     * Nullable so an existing table can gain the column before old rows are removed.
     * Writers always set the branch.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Branch branch;

    @Column(nullable = false, length = 80)
    String name;

    @Column(length = 80)
    String series;

    @Column(length = 40)
    String viscosity;

    /** eded, litr, kq, metr, dest, servis. Labels stay in the UI. */
    @Column(nullable = false, length = 16)
    String unit;
}
