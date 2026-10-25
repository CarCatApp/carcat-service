package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Kataloq sətirini bir yakıta bağlamaq.
 * en: Link a catalog row to one fuel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFuelIntervalAddRequest {
    Long engineTypeId;
    Long individualServiceId;
    Long intervalKm;
    Integer intervalMonth;
}
