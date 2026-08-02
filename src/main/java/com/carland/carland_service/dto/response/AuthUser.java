package com.carland.carland_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * tr: Kimlik doğrulama/login akışında dönen kullanıcı bilgilerini (id, telefon, durum, ad-soyad) taşıyan yanıt DTO'su.
 * en: Response DTO carrying authenticated user info (id, phone, status, name-surname) returned in the auth/login flow.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthUser {
    Long id;
    LocalDateTime createdAt;
    String phoneNumber;
    String status;
    String name;
    String surname;
}
