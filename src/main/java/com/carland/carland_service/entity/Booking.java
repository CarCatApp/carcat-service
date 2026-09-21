package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

/**
 * tr: Owner-app rezervasyonu. slot = ranges.range_id. Para qəpik.
 * en: Owner-app booking. slot = ranges.range_id. Money in qepik.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_branch_status", columnList = "branch_id, status"),
                @Index(name = "idx_bookings_range", columnList = "range_id"),
                @Index(name = "idx_bookings_customer", columnList = "customer_user_id")
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 16)
    String ref;

    @Column(name = "customer_user_id", nullable = false)
    Long customerUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "range_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Range range;

    @Column(nullable = false, length = 16)
    String status;

    @Column(name = "price_min")
    Integer priceMin;

    @Column(name = "price_max")
    Integer priceMax;

    @Column(nullable = false, length = 8)
    @Builder.Default
    String currency = "AZN";

    @Column(length = 32)
    String vin;

    @Column(name = "car_id")
    Long carId;

    @Column(name = "cancel_reason_code", length = 64)
    String cancelReasonCode;

    @Column(name = "cancel_note", length = 500)
    String cancelNote;

    @Column(name = "pending_expires_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime pendingExpiresAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (currency == null) {
            currency = "AZN";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
