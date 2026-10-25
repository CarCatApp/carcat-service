package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * tr: "partner_badge_logos" tablosunu modelleyen eski entity. Yeni rozetler {@code branch_badge_logos} tablosundadır.
 * en: Legacy entity for "partner_badge_logos". New badges live in {@code branch_badge_logos}.
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
