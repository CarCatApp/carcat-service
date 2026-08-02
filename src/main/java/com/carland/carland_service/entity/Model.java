package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: "models" tablosunu modelleyen entity; bir markaya bağlı araç modeli sözlük kaydını temsil eder.
 * en: Entity modeling the "models" table; represents a car model lookup record belonging to a brand.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "models")
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long modelId;
    String modelName;
    Long brandId;
    String status;
    String isnew;

}
