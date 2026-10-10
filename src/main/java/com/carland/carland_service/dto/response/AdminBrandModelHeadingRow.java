package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin marka başlığı satırı.
 * en: Admin brand-heading row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBrandModelHeadingRow {
    Long id;
    String titleAz;
    String titleEn;
    String titleRu;
    Integer sortOrder;
    Boolean oil;
}
