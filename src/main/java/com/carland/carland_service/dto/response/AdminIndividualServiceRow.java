package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin fərdi xidmət tablosu satırı.
 * en: Admin individual-service table row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminIndividualServiceRow {
    Long id;
    String code;
    String titleAz;
    String titleEn;
    String titleRu;
    Integer sortOrder;
    Boolean active;
}
