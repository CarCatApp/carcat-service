package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin panel kategori kaydı. id null ise yeni kayıt; code yalnızca yeni kayıtta yazılır.
 * en: Admin panel category save. Null id creates a row; code is written only on create.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminServiceCategorySaveRequest {
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
}
