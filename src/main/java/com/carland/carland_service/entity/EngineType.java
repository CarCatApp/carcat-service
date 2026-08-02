package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: "engine_types" tablosunu modelleyen entity; motor tipi sözlük kaydını (benzin, dizel vb.) temsil eder.
 * en: Entity modeling the "engine_types" table; represents an engine type lookup record (petrol, diesel, etc.).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "engine_types")
public class EngineType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long engineTypeId;
    String engineType;
    String status;

}
