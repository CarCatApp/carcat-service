package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * tr: Booking şubesi (adres + koordinat). Hyper {@code partners} ve eski {@code auto_services} tablolarından ayrıdır.
 * en: Booking branch (address + coordinates). Separate from Hyper {@code partners} and legacy {@code auto_services}.
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

    @Column(name = "contact_phone", length = 32)
    String contactPhone;

    @Column(name = "working_hours", length = 512)
    String workingHours;

    @Column(length = 512)
    String photo;

    /** Extra photo URLs, one per line. */
    @Column(length = 2000)
    String photos;

    Double rating;

    @Builder.Default
    @Column(name = "rating_count")
    Integer ratingCount = 0;

    @Builder.Default
    @Column(nullable = false)
    Boolean active = true;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (active == null) {
            active = true;
        }
        if (ratingCount == null) {
            ratingCount = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
