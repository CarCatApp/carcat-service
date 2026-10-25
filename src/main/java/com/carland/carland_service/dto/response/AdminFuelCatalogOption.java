package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Bu yakıta hələ bağlanmamış aktiv kataloq sətri.
 * en: Active catalog row not yet linked to this fuel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFuelCatalogOption {
    Long id;
    String code;
    String titleAz;
}
