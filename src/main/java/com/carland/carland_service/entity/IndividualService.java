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
 * tr: Ortak fərdi xidmət kataloqu. Şube fiyatı burada durmaz.
 * en: Shared individual-service catalog. Branch prices are not stored here.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "individual_services",
        uniqueConstraints = @UniqueConstraint(name = "uk_individual_services_code", columnNames = "code")
)
public class IndividualService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** MYF, SLF, RZV */
    @Column(nullable = false, length = 8)
    String code;

    /** {"az":"...","en":"...","ru":"..."} */
    @Column(name = "title_json", nullable = false, length = 1024)
    String titleJson;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;

    /** Catalog switch. False hides the line from every branch. */
    @Column(nullable = false)
    @Builder.Default
    Boolean active = true;
}
