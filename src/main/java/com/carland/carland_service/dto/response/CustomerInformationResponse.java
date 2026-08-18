package com.carland.carland_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: "Məlumatlarım" ekranı için müşteri profil yanıtı; telefon salt-okunur döner.
 * en: Customer profile response for the "My information" screen; phone is returned read-only.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerInformationResponse {
    String name;
    String surname;
    String mail;
    @Schema(description = "FIN (personal identification number)")
    String pin;
    @Schema(description = "Read-only. Phone cannot be changed from this screen.")
    String phoneNumber;
}
