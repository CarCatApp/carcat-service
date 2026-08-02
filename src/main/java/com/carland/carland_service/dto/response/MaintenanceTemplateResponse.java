package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: MaintenanceTemplateController sorgularında dönen, motor tipine bağlı bakım şablonunu ve servis listesini içeren yanıt DTO'su.
 * en: Response DTO returned by MaintenanceTemplateController queries, containing the engine-type-based maintenance template and its service list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceTemplateResponse {
    Long id;
    String name;
    String engineType;
    Long engineTypeId;
    String message;
    List<ServiceResponse> serviceResponseList;
}
