package com.carland.carland_service.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


/**
 * tr: "colors" tablosunu modelleyen entity; araç rengi sözlük kaydını temsil eder.
 * en: Entity modeling the "colors" table; represents a car color lookup record.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "colors")
public class Color {

    @Id
    Long colorId;
    String color;


}

