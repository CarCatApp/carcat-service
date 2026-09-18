package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * tr: "percentage_empty_photos" tablosu; servis ikonu yokken GET'in döndürdüğü tek placeholder görseli.
 * en: "percentage_empty_photos" table; the single placeholder image returned by GET when a service has no icon.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "percentage_empty_photos")
public class PercentageEmptyPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;

    String fileName;
    String fileType;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    byte[] imageData;
}
