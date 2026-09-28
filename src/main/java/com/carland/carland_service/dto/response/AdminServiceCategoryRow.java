package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin kategori tablosu satırı. Başlıklar ayrı az/en/ru alanları.
 * en: Admin category table row. Titles are separate az/en/ru fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminServiceCategoryRow {
    Long id;
    String code;
    String titleAz;
    String titleEn;
    String titleRu;
    String descriptionAz;
    String descriptionEn;
    String descriptionRu;
    Integer sortOrder;
    Boolean openable;
    Boolean toggleable;
    Boolean active;
    String directionKeys;
    Integer iconVersion;
    Boolean hasIcon;
}
