package com.carland.carland_service.dto.response;

import lombok.*;

import java.time.LocalDate;


/**
 * tr: Müşteri bilgilerini (id, telefon, ad-soyad, durum) döndüren yanıt DTO'su; kullanıcı ve admin sorgularında kullanılır.
 * en: Response DTO returning customer info (id, phone, name-surname, status); used in user and admin queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    Long userId;
    String phoneNumber;
    String name;
    String surname;
    String status;
    LocalDate createdAt;

}
