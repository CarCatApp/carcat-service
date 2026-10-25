package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * tr: Şube rozeti. Mobil {@code GET /api/v1/photo/for/branch/badge-logo/get/{branchId}}.
 * en: Branch badge. Mobile {@code GET /api/v1/photo/for/branch/badge-logo/get/{branchId}}.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "branch_badge_logos")
public class BranchBadgeLogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;

    @Column(name = "branch_id", nullable = false, unique = true)
    Long branchId;

    String fileName;
    String fileType;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    byte[] imageData;
}
