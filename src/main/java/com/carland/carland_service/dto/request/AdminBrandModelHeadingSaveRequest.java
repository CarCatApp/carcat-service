package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin marka başlığı. Üç dil de dolu olur. oil true ise şube satırında özüllük zorunlu.
 * en: Admin brand heading. All three languages are filled. oil true requires viscosity on a branch row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBrandModelHeadingSaveRequest {
    Long id;
    String titleAz;
    String titleEn;
    String titleRu;
    Integer sortOrder;
    Boolean oil;
}
