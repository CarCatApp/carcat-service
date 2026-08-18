package com.carland.carland_service.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: "Məlumatlarım" ekranından müşteri ad, soyad, e-posta ve FIN kodunu kaydetmek için istek DTO'su. Telefon gönderilmez / yok sayılır.
 * en: Request DTO to save customer name, surname, e-mail and FIN from the "My information" screen. Phone is not accepted / ignored.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerInformationRequest {

    @Schema(example = "Aziz")
    String name;

    @Schema(example = "Mammadov")
    String surname;

    @JsonAlias("email")
    @Schema(example = "aziz@example.com")
    String mail;

    /** FIN — SIMA field name is pin. Not the login PIN. */
    @Schema(description = "FIN (personal identification number)", example = "62HJ5KQ")
    String pin;
}
