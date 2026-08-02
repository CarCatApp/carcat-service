package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Servis tanımı (çok dilli ad, bakım aralığı km/ay) oluşturma ve güncelleme isteklerinde kullanılan DTO.
 * en: DTO used in requests to create and update a service definition (multilingual name, maintenance interval in km/months).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceRequest {

    String serviceName;

    String actionType;

    String nameAz;

    String nameEn;

    String nameRu;

    Long intervalKm;

    Integer intervalMonth;

}