package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * tr: Dövri qulluq paket tablosundaki grup (Dəyişdirmə, Əlavə etmə, …). Başlıklar az/en/ru JSON.
 * en: Group row on the routine-care package table (Replace, Top up, …). Titles are az/en/ru JSON.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "service_behaviors",
        uniqueConstraints = @UniqueConstraint(name = "uk_service_behaviors_code", columnNames = "code")
)
public class ServiceBehavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** replace | extra | inspect | service ; ^[a-z0-9_]+$ */
    @Column(nullable = false, length = 64)
    String code;

    /** {"az":"...","en":"...","ru":"..."} */
    @Column(name = "title_json", nullable = false, length = 1024)
    String titleJson;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    Boolean active = true;
}
