package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: PartnerController üzerinden iş ortağı (partner) oluşturma ve güncelleme isteklerinde kullanılan DTO. Webhook secret ve API kimlik bilgilerini de içerir.
 * en: DTO used in PartnerController requests for creating and updating a partner. Also includes webhook secret and API credentials.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PartnerRequest {
    Long id;
    String name;
    String dealer;
    String logoUrl;
    Boolean active;
    String source;
    String webhookSecret;
    String apiClientId;
    String apiClientSecret;
}
