package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin xidmət tablosu satırı.
 * en: Admin service table row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOfferedServiceRow {
    Long id;
    Long behaviorId;
    String behaviorCode;
    String behaviorTitleAz;
    Long filterId;
    String filterName;
    String titleAz;
    String titleEn;
    String titleRu;
    Integer sortOrder;
    Boolean active;
}
