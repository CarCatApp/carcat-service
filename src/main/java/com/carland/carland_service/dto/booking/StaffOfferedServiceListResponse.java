package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Gruplanmış xidmət listesi.
 * en: Grouped service list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOfferedServiceListResponse {
    List<StaffServiceBehaviorGroupView> groups;
}
