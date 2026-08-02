package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: AutoServiceController üzerinden servis merkezi (avtoservis) oluşturma ve güncelleme isteklerinde kullanılan DTO. Ad, adres ve iletişim bilgilerini taşır.
 * en: DTO used in AutoServiceController requests for creating and updating an auto service center. Carries name, address and contact details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoServiceRequest {
    Long id;

    String name;
    String address;
    String phoneNumber;
    String email;

}
