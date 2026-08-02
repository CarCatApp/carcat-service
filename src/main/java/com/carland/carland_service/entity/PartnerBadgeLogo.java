package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * tr: "partner_badge_logos" tablosunu modelleyen entity; iş ortağının rozet (badge) logosunu ikili veri olarak saklar.
 * en: Entity modeling the "partner_badge_logos" table; stores a partner's badge logo as binary data.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "partner_badge_logos")
public class PartnerBadgeLogo {
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
