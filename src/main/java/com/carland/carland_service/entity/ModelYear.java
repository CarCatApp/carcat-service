package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: "model_years" tablosunu modelleyen entity; araç model yılı sözlük kaydını temsil eder.
 * en: Entity modeling the "model_years" table; represents a car model year lookup record.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "model_years")
public class ModelYear {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long modelYearId;
    Integer modelYear;
    String status;

}
