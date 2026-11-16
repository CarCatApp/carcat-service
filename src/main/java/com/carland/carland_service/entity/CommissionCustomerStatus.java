package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * tr: VIN başına Hyper loyalty. new / reactivated yapışır. Yalnız old → reactivated ola bilər.
 * en: Hyper loyalty per VIN. new / reactivated stick. Only old may become reactivated.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "commission_customer_status", uniqueConstraints = {
        @UniqueConstraint(name = "uk_commission_customer_vin", columnNames = "vin")
})
public class CommissionCustomerStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 32)
    String vin;

    /** new | reactivated | old */
    @Column(nullable = false, length = 16)
    String status;

    @Column(name = "decided_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime decidedAt;

    @Column(name = "first_booking_id")
    Long firstBookingId;

    /** ACTIVE | INACTIVE_365 | ALREADY_REACTIVATED | … */
    @Column(name = "hyper_outcome", nullable = false, length = 32)
    String hyperOutcome;

    /** tr: Son qərarda istifadə olunan son xidmət tarixi. en: Last service date used by the latest decision. */
    @Column(name = "earliest_service_date")
    LocalDate earliestServiceDate;

    @Column(length = 2000)
    String snapshot;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (decidedAt == null) {
            decidedAt = createdAt;
        }
    }
}
