package com.carland.carland_service.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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

    static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    LocalDate created;
    /**
     * tr: Push anı Bakü yerel saatiyle durur. Cevapta yalnızca saat gider: 23:34.
     * en: The push instant is stored in Baku local time. The response sends only the clock: 23:34.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;
    String type;
    String notificationText;
    String title;
    Long customerId;
    String status;
    boolean isRead;

    /**
     * tr: Bildirim anı. Sunucu UTC olsa da duvar saati Bakü'dür.
     * en: Notification instant. The wall clock is Baku even when the server is UTC.
     */
    public static OffsetDateTime nowLocal() {
        return OffsetDateTime.now(BAKU);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = nowLocal();
        }
        if (created == null) {
            created = createdAt.atZoneSameInstant(BAKU).toLocalDate();
        }
    }
}
