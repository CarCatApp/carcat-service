package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: "super_admins" tablosunu modelleyen entity; bir servis merkezinin sahibi/yöneticisi olan superadmin kullanıcısını temsil eder.
 * en: Entity modeling the "super_admins" table; represents the superadmin user who owns/manages an auto service center.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "super_admins")
public class SuperAdmin {

    @Id
    Long userId;

    String phoneNumber;
    String name;
    String surname;
    String notificationLanguage;
    String status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auto_service_id", unique = true)
    AutoService autoService;
}

