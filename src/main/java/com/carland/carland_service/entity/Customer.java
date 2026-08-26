package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

/**
 * tr: "customers" tablosunu modelleyen entity; uygulama müşterisini (telefon, ad-soyad, e-posta, FIN, durum) ve sahip olduğu araçları temsil eder.
 * en: Entity modeling the "customers" table; represents an app customer (phone, name-surname, email, FIN, status) and the cars they own.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "customers")
public class Customer {
    @Id
    Long userId;
    String phoneNumber;
    String name;
    String surname;
    /** Customer e-mail; unique when set. Not the auth login identifier. */
    @Column(unique = true)
    String mail;
    /**
     * FIN (Azerbaijan personal ID). JSON/API name is {@code pin} to match SIMA.
     * This is not the login PIN stored as pin_hash on auth users.
     */
    @Column(name = "pin", unique = true, length = 7)
    String pin;
    /**
     * True after a SIMA citizen/foreign attempt passed isSuccess + both scores &gt;= 0.90.
     * Locks name, surname and FIN on PUT /user/information.
     */
    @Column(name = "sima_verified", nullable = false)
    @Builder.Default
    Boolean simaVerified = false;
    String notificationLanguage;
    String status;
    LocalDate createdAt;
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @ToString.Exclude
    List<Car> cars;
}
