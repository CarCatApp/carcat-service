package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * tr: Paketteki bir xidmət. enabled false ise satır durur ama pakette sayılmaz.
 * en: One service on a package. enabled false keeps the row out of the package.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCarePackageItemView {
    Long offeredServiceId;
    Map<String, String> title;
    Boolean enabled;
    Long behaviorId;
    String behaviorCode;
    Map<String, String> behaviorTitle;
    Integer sortOrder;
}
