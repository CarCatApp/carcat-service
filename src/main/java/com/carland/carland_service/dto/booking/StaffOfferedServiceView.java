package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * tr: Partner paneline dönen tek xidmət satırı.
 * en: One service line returned to the partner panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOfferedServiceView {
    Long id;
    Map<String, String> title;
    Integer sortOrder;
}
