package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Seçilmiş yakıtın şablonundakı bir kalem.
 * en: One row of the selected fuel's maintenance template.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFuelIntervalRow {
    Long serviceId;
    String titleAz;
    String titleEn;
    String titleRu;
    Long intervalKm;
    Integer intervalMonth;
    /** tr: Aynı kataloq sətirinin durduğu digər yakıtlar. / en: Other fuels that already have this catalog row. */
    String otherFuels;
}
