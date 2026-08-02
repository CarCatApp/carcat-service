package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * tr: "brands" tablosunu modelleyen entity; araç markası sözlük kaydını ve (transient) model listesini temsil eder.
 * en: Entity modeling the "brands" table; represents a car brand lookup record and its (transient) model list.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "brands")
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long brandId;
    String brandName;
    String status;
    String isnew;

    @Transient
    List<Model> models;
}
