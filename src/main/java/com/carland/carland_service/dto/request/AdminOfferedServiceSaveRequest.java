package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin xidmət kaydı. behaviorId seçilen grubun id'sidir.
 * en: Admin service save. behaviorId is the id of the selected group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOfferedServiceSaveRequest {
    Long id;
    Long behaviorId;
    Long filterId;
    String titleAz;
    String titleEn;
    String titleRu;
    Integer sortOrder;
    Boolean active;
}
