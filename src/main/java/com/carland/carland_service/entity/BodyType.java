package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: "body_types" tablosunu modelleyen entity; araç kasa tipi sözlük kaydını (sedan, SUV vb.) temsil eder.
 * en: Entity modeling the "body_types" table; represents a car body type lookup record (sedan, SUV, etc.).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "body_types")
public class BodyType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long bodyTypeId;
    String bodyType;
    String status;
}
