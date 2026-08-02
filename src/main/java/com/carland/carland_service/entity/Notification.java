package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * tr: "notifications" tablosunu modelleyen entity; müşteriye gönderilen bildirimi (tip, metin, okunma durumu) saklar.
 * en: Entity modeling the "notifications" table; stores a notification sent to a customer (type, text, read status).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    LocalDate created;
    String type;
    String notificationText;
    String title;
    Long customerId;
    String status;
    boolean isRead;
}
