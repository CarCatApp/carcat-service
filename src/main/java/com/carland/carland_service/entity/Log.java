package com.carland.carland_service.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


/**
 * tr: "logs" tablosunu modelleyen entity; kullanıcı bazlı uygulama log kayıtlarını saklar.
 * en: Entity modeling the "logs" table; stores per-user application log entries.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "logs")
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long logId;
    String userId;
    String log;
}

