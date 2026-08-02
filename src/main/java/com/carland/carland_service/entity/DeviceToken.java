package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: "device_tokens" tablosunu modelleyen entity; kullanıcının push bildirim cihaz token'ını ve platform bilgisini saklar.
 * en: Entity modeling the "device_tokens" table; stores a user's push notification device token and platform info.
 */
@Entity
@Table(name = "device_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    Long userId;

    @Column(name = "device_token", nullable = false, unique = true)
    String deviceToken;

    @Column(name = "platform", nullable = false, length = 20)
    String platform;


}
