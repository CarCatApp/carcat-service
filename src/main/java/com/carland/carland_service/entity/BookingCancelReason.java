package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: İptal sebebi kataloğu. API CRCT-285.
 * en: Cancel-reason catalogue. API in CRCT-285.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "booking_cancel_reasons")
public class BookingCancelReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 64)
    String code;

    @Column(name = "title_json", nullable = false, length = 1024)
    String titleJson;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    Boolean active = true;
}
