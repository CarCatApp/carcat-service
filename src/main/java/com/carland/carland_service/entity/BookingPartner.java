package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Booking organizasyonu (HQ). Hyper entegrasyon {@code partners} tablosundan ayrıdır.
 * en: Booking organization (HQ). Separate from the Hyper integration {@code partners} table.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "booking_partners")
public class BookingPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    @Builder.Default
    @Column(nullable = false)
    Boolean active = true;

    /** Auth users.id of the single HQ / partner admin. */
    @Column(name = "hq_user_id", unique = true)
    Long hqUserId;

    LocalDateTime createdAt;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<BookingBranch> branches = new ArrayList<>();
}
