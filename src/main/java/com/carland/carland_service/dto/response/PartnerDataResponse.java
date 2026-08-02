package com.carland.carland_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: İş ortağı (partner) temel bilgilerini (ad, bayi, logo, aktiflik) taşıyan yanıt DTO'su; PartnerResponse ve servis geçmişi yanıtlarında kullanılır.
 * en: Response DTO carrying core partner info (name, dealer, logo, active flag); used in PartnerResponse and service history responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerDataResponse {
    Long id;
    String name;
    String dealer;
    String logoUrl;
    Boolean active;
    String source;
}
