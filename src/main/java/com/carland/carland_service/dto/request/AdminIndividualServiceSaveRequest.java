package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin fərdi xidmət kaydı. Kod bütün şubeler için ortaktır.
 * en: Admin individual-service save. The code is shared by every branch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminIndividualServiceSaveRequest {
    Long id;
    String code;
    String titleAz;
    String titleEn;
    String titleRu;
    Integer sortOrder;
    Boolean active;
    Long filterId;
}
