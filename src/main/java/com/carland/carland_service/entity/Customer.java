package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

/**
 * tr: "customers" tablosunu modelleyen entity; uygulama müşterisini (telefon, ad-soyad, durum) ve sahip olduğu araçları temsil eder.
 * en: Entity modeling the "customers" table; represents an app customer (phone, name-surname, status) and the cars they own.
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
    String notificationLanguage;
    String status;
    LocalDate createdAt;
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @ToString.Exclude
    List<Car> cars;
}
