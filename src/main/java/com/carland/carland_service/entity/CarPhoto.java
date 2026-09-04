package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * tr: "photos" tablosunu modelleyen entity; bir araca ait fotoğrafı ikili (binary) veri olarak saklar.
 * en: Entity modeling the "photos" table; stores a car's photo as binary data.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "photos")
public class CarPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;
    Long carId;
    String fileName;
    String fileType;
    /** pending | ready | failed — belongs to the photo, not the car. */
    String photoStatus;
    /** ai_generated | user | default */
    String photoSource;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    byte[] imageData;
}
