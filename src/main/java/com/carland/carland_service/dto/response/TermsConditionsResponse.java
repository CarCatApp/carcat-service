package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: LegalController kullanım koşulları endpoint'inde dönen, koşul metni bölümlerini ve şirket bilgilerini içeren yanıt DTO'su.
 * en: Response DTO returned by the LegalController terms and conditions endpoint, containing terms text sections and company info.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermsConditionsResponse {
    private String lastUpdated;
    private String version;
    private String language;
    private String company;
    private String companyAz;
    private String email;
    private String website;
    private String location;
    private String country;

    private String headerTitle;
    private String headerSubtitle;

    private List<String> sections;

    private String acceptanceText;
}
