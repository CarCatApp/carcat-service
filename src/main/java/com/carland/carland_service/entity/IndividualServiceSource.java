package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * tr: percentages serviceId (services.id) → fərdi kataloq satırı.
 *     Eyni üç dil adı bir neçə şablon satırıdır; hamısı eyni individual_services.id-yə bağlanır.
 * en: percentages serviceId (services.id) to one individual-catalog row.
 *     The same three names can be several template rows; all of them share one individual_services.id.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "individual_service_sources")
public class IndividualServiceSource {

    /** services.id. Assigned, not generated. */
    @Id
    @Column(name = "service_id", nullable = false)
    Long serviceId;

    @Column(name = "individual_service_id", nullable = false)
    Long individualServiceId;
}
