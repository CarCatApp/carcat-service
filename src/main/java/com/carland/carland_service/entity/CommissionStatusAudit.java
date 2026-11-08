package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * tr: Hər müştəri-status yoxlamasının dəyişməz yazısı. Sətir yenilənmir.
 * en: Immutable row for one customer-status check. Rows are not updated.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "commission_status_audit")
public class CommissionStatusAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(length = 32)
    String vin;

    @Column(name = "booking_id")
    Long bookingId;

    @Column(name = "booking_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime bookingTime;

    @Column(name = "request_text", length = 4000)
    String requestText;

    @Column(name = "response_text", length = 4000)
    String responseText;

    @Column(name = "last_service_date")
    LocalDate lastServiceDate;

    /** new | old */
    @Column(length = 8)
    String status;

    @Column(length = 16)
    String reason;

    @Column(name = "review_required", nullable = false)
    boolean reviewRequired;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
