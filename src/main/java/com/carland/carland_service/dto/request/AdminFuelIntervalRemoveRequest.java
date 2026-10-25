package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Kalemi yalnız seçilmiş yakıtın şablonundan çıxarmaq.
 * en: Remove the row from the selected fuel's template only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFuelIntervalRemoveRequest {
    Long serviceId;
}
