package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
}
