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
 * tr: VIN başına bir kez yazılan müşteri durumu. Sonra değişmez.
 * en: Customer status written once per VIN. It does not change later.
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

    /** new | old */
    @Column(nullable = false, length = 8)
    String status;

    @Column(name = "decided_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime decidedAt;

    @Column(name = "first_booking_id")
    Long firstBookingId;

    /** found | not_found | timeout */
    @Column(name = "hyper_outcome", nullable = false, length = 16)
    String hyperOutcome;

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
