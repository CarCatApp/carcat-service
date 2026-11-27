package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

/**
 * tr: Şubenin müşteriye gönderdiği saat teklifi. Kabul edilene kadar rezervasyonun saati değişmez.
 * en: A branch time offer. The booking hour stays until the customer accepts.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "booking_time_proposals")
public class BookingTimeProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Booking booking;

    @Column(name = "message_id")
    Long messageId;

    @Column(name = "from_range_id")
    Long fromRangeId;

    @Column(name = "to_range_id", nullable = false)
    Long toRangeId;

    /** pending | accepted | declined | superseded */
    @Column(nullable = false, length = 16)
    String status;

    @Column(name = "created_by_user_id")
    Long createdByUserId;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;

    @Column(name = "decided_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime decidedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "pending";
        }
    }
}
