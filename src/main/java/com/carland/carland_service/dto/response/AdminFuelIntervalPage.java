package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Admin yakıt aralığı səhifəsinin modeli.
 * en: Model for the admin fuel-interval page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFuelIntervalPage {
    @Builder.Default
    List<AdminFuelTypeOption> fuels = List.of();
    Long selectedEngineTypeId;
    String selectedLabel;
    String templateName;
    boolean templateMissing;
    @Builder.Default
    List<AdminFuelIntervalRow> rows = List.of();
    @Builder.Default
    List<AdminFuelCatalogOption> catalogOptions = List.of();
}
