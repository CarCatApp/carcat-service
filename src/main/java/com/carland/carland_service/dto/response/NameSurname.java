package com.carland.carland_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Kullanıcının ad ve soyad bilgisini taşıyan basit yanıt DTO'su.
 * en: Simple response DTO carrying a user's name and surname.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NameSurname {
    String name;
    String surname;
}
