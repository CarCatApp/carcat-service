package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Servis tanımını (çok dilli ad, bakım aralığı km/ay, önem durumu) döndüren yanıt DTO'su; bakım şablonu yanıtlarında kullanılır.
 * en: Response DTO returning a service definition (multilingual name, maintenance interval in km/months, importance flag); used in maintenance template responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponse {
    Long id;
    String serviceName;
    String actionType;
    Long intervalKm;
    Integer intervalMonth;
    Double amount;
    String nameAz;//    name_az
    String nameEn;//    name_en
    String nameRu;//    name_ru
    boolean important;
}
