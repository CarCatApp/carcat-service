package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;

/**
 * tr: Ortak marka başlığı. Şubeye bağlı değil. oil true ise satırlarda özüllük zorunlu.
 * en: Shared brand heading. Not tied to a branch. When oil is true, rows require viscosity.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "brand_model_services")
public class BrandModelService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** {"az":"...","en":"...","ru":"..."}. Nullable so an existing table can gain the column before old rows are removed. */
    @Column(name = "title_json", length = 1024)
    String titleJson;

    @Column(name = "oil", nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    Boolean oil = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;
}
