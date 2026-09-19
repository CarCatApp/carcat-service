package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * tr: Booking staff defteri — kim, ne zaman, ne (şifre/UUID yok).
 * en: Booking staff ledger — who, when, what (no password/UUID).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "booking_staff_audit")
public class BookingStaffAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(nullable = false, length = 64)
    String actor;

    @Column(nullable = false, length = 32)
    String action;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "partner_id")
    Long partnerId;

    @Column(name = "branch_id")
    Long branchId;

    @Column(name = "phone_number", length = 32)
    String phoneNumber;

    @Column(length = 32)
    String role;

    @Column(length = 256)
    String detail;

    Boolean success;
}
