package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * tr: Bir davranış grubu ve altındaki xidmətlər.
 * en: One behavior group and the services under it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffServiceBehaviorGroupView {
    Long id;
    String code;
    Map<String, String> title;
    Integer sortOrder;
    List<StaffOfferedServiceView> services;
}
