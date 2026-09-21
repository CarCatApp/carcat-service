package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Booking satırı — bir serviceKey.
 * en: Booking line — one serviceKey.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "booking_items")
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Booking booking;

    @Column(name = "service_key", nullable = false, length = 64)
    String serviceKey;

    @Column(name = "title_snapshot", length = 512)
    String titleSnapshot;

    @Column(name = "price_min")
    Integer priceMin;

    @Column(name = "price_max")
    Integer priceMax;
}
