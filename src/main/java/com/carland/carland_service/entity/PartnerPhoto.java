package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * tr: "partner_photos" tablosunu modelleyen entity; iş ortağına ait fotoğrafı/logoyu ikili veri olarak saklar.
 * en: Entity modeling the "partner_photos" table; stores a partner's photo/logo as binary data.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "partner_photos")
public class PartnerPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;
    Long partnerId;
    String fileName;
    String fileType;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    byte[] imageData;
}
