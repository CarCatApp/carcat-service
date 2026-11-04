package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * tr: Partner + durum + yürürlük tarihi. Eski satır güncellenmez, yeni satır eklenir.
 * en: Partner + status + effective date. An old row is not updated; a new row is inserted.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "commission_rates", indexes = {
        @Index(name = "idx_commission_rates_lookup", columnList = "partner_id, customer_status, effective_from")
})
public class CommissionRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "partner_id", nullable = false)
    Long partnerId;

    /** new | old */
    @Column(name = "customer_status", nullable = false, length = 8)
    String customerStatus;

    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
    BigDecimal percent;

    @Column(name = "effective_from", nullable = false)
    LocalDate effectiveFrom;

    @Column(length = 200)
    String note;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
