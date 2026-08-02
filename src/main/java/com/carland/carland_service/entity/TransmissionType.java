package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: "transmission_types" tablosunu modelleyen entity; vites tipi sözlük kaydını (manuel, otomatik vb.) temsil eder.
 * en: Entity modeling the "transmission_types" table; represents a transmission type lookup record (manual, automatic, etc.).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "transmission_types")
public class TransmissionType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long transmissionTypeId;
    String transmissionType;
    String status;

}
