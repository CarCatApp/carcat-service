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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * tr: Bir book için təmir və yoxlanış kaydı. Metin ve şube kopyası burada durur.
 * en: One repair-and-inspection record per booking. The message and branch copy live here.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "booking_inspections",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_inspections_booking", columnNames = "booking_id")
)
public class BookingInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Booking booking;

    @Column(name = "customer_user_id", nullable = false)
    Long customerUserId;

    @Column(name = "customer_name", length = 160)
    String customerName;

    @Column(name = "customer_phone", length = 32)
    String customerPhone;

    @Column(name = "range_id", nullable = false)
    Long rangeId;

    @Column(name = "slot_day")
    LocalDate slotDay;

    @Column(name = "slot_start", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime slotStart;

    @Column(name = "branch_id", nullable = false)
    Long branchId;

    @Column(name = "branch_name", length = 160)
    String branchName;

    @Column(name = "partner_id")
    Long partnerId;

    @Column(name = "booking_ref", length = 16)
    String bookingRef;

    @Column(nullable = false, length = 500)
    String message;

    @Column(length = 32)
    String vin;

    @Column(name = "car_id")
    Long carId;

    @Column(name = "plate_number", length = 32)
    String plateNumber;

    @Column(name = "car_brand", length = 80)
    String carBrand;

    @Column(name = "car_model", length = 80)
    String carModel;

    @Column(name = "car_year")
    Integer carYear;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
