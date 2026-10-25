package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Yakıt aralığı səhifəsinin üst düyməsi.
 * en: Fuel button on the interval admin page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFuelTypeOption {
    Long engineTypeId;
    String label;
}
