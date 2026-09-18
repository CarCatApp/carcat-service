package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * tr: "percentage_photos" tablosu; bakım şablonundaki bir servis kaleminin (services.id) ikonunu saklar.
 * en: "percentage_photos" table; stores the icon for one maintenance-template service row (services.id).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "percentage_photos",
        uniqueConstraints = @UniqueConstraint(name = "uk_percentage_photos_service_id", columnNames = "service_id")
)
public class PercentagePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;

    @Column(name = "service_id", nullable = false)
    Long serviceId;

    String fileName;
    String fileType;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    byte[] imageData;
}
