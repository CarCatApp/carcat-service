package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin grup kaydı. id null ise yeni kayıt; code yalnızca yeni kayıtta yazılır.
 * en: Admin group save. Null id creates a row; code is written only on create.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminServiceBehaviorSaveRequest {
    Long id;
    String code;
    String titleAz;
    String titleEn;
    String titleRu;
    Integer sortOrder;
    Boolean active;
}
