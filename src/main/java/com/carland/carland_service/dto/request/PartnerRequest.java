package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Partner oluşturma/güncelleme DTO'su. Secret kolon yok (env).
 * en: Partner create/update DTO. No secret columns (env).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PartnerRequest {
    Long id;
    String name;
    String logoUrl;
    Boolean active;
    String source;
}
