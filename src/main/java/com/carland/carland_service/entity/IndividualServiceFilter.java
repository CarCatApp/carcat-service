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
 * tr: Fərdi xidmət filter chip'i. All satır deyil; Flutter filtresiz listeyi kendisi gösterir.
 * en: Individual-service filter chip. All is not a row; Flutter shows the unfiltered list itself.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "individual_service_filters",
        uniqueConstraints = @UniqueConstraint(name = "uk_individual_service_filters_name", columnNames = "name")
)
public class IndividualServiceFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Filters, Brakes, Fluids, Tyres, Ignition, Engine, Electrical */
    @Column(nullable = false, length = 64)
    String name;
}
