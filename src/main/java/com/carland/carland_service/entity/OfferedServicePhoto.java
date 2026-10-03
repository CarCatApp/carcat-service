package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * tr: offered_service_photos. Bir offered service satırının ikonu.
 * en: offered_service_photos. Icon for one offered-service row.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "offered_service_photos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_offered_service_photos_offered_service_id",
                columnNames = "offered_service_id"
        )
)
public class OfferedServicePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;

    @Column(name = "offered_service_id", nullable = false)
    Long offeredServiceId;

    String fileName;
    String fileType;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    byte[] imageData;
}
