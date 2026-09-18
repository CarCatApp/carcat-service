package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * tr: Booking şubesi (adres + koordinat). Hastane {@code auto_services} tablosundan ayrıdır.
 * en: Booking branch (address + coordinates). Separate from hospital {@code auto_services}.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "booking_branches")
public class BookingBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BookingPartner partner;

    @Column(nullable = false)
    String name;

    String address;

    Double lat;

    Double lng;

    @Builder.Default
    @Column(nullable = false)
    Boolean active = true;

    LocalDateTime createdAt;
}
