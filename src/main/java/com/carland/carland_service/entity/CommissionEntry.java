package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
 * tr: Tamamlanmış bir randevunun komisyon satırı. Oran burada donar. Tutar sonra gelebilir.
 * en: Commission row of one completed booking. The rate freezes here. The amount may arrive later.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "commission_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_commission_entries_booking", columnNames = "booking_id"),
        indexes = {
                @Index(name = "idx_commission_entries_completed", columnList = "completed_at"),
                @Index(name = "idx_commission_entries_vin", columnList = "vin")
        })
public class CommissionEntry {

    public static final String READY = "ready";
    public static final String PENDING = "pending";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "booking_id", nullable = false)
    Long bookingId;

    @Column(length = 16)
    String ref;

    @Column(name = "partner_id")
    Long partnerId;

    @Column(name = "partner_name", length = 160)
    String partnerName;

    @Column(name = "branch_id")
    Long branchId;

    @Column(name = "branch_name", length = 160)
    String branchName;

    @Column(length = 32)
    String vin;

    @Column(length = 32)
    String plate;

    @Column(name = "completed_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime completedAt;

    /** new | old */
    @Column(name = "customer_status", nullable = false, length = 8)
    String customerStatus;

    @Column(name = "status_decided_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime statusDecidedAt;

    @Column(name = "hyper_outcome", length = 16)
    String hyperOutcome;

    @Column(name = "earliest_service_date")
    LocalDate earliestServiceDate;

    @Column(name = "hyper_snapshot", length = 2000)
    String hyperSnapshot;

    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
    BigDecimal ratePercent;

    @Column(name = "service_amount", precision = 12, scale = 2)
    BigDecimal serviceAmount;

    @Column(name = "commission_amount", precision = 12, scale = 2)
    BigDecimal commissionAmount;

    /** ready | pending */
    @Column(name = "amount_state", nullable = false, length = 16)
    String amountState;
}
