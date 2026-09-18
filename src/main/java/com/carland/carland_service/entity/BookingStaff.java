package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * tr: Booking üyelik satırı. {@code branch} null ise HQ / partner admin.
 * en: Booking membership row. Null {@code branch} means HQ / partner admin.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "booking_staff",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_staff_user_branch",
                columnNames = {"user_id", "branch_id"}
        )
)
public class BookingStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BookingPartner partner;

    /** Null = partner admin (all branches). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BookingBranch branch;

    @Column(nullable = false, length = 32)
    String role;

    @Column(nullable = false, length = 32)
    String status;

    @Column(nullable = false)
    String phoneNumber;

    String name;
    String surname;

    LocalDateTime createdAt;
}
