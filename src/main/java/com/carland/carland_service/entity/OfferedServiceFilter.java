package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * tr: Paket satırının Flutter grubu. All satır deyil. Fərdi xidmət filterindən ayrıdır.
 * en: Flutter group for a care-package line. All is not a row. Separate from individual-service filters.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "offered_service_filters",
        uniqueConstraints = @UniqueConstraint(name = "uk_offered_service_filters_name_en", columnNames = "name_en")
)
public class OfferedServiceFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name_az", nullable = false, length = 64)
    String nameAz;

    @Column(name = "name_en", nullable = false, length = 64)
    String nameEn;

    @Column(name = "name_ru", nullable = false, length = 64)
    String nameRu;
}
