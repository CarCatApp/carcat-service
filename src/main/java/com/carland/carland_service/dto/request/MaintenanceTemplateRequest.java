package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: MaintenanceTemplateController üzerinden motor tipine göre bakım şablonu sorgulama/oluşturma isteklerinde kullanılan DTO.
 * en: DTO used in MaintenanceTemplateController requests to query/create a maintenance template by engine type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceTemplateRequest {

    String engineType;

}
