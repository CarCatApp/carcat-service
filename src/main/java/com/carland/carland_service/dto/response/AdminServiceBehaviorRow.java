package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin grup tablosu satırı.
 * en: Admin group table row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminServiceBehaviorRow {
    Long id;
    String code;
    String titleAz;
    String titleEn;
    String titleRu;
    Integer sortOrder;
    Boolean active;
}
