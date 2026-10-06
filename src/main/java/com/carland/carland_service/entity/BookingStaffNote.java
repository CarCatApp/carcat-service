package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * tr: Partner rədd və gəlmədi səbəb kataloqu. Mətn az/en/ru.
 * en: Partner reject and no-show reason catalogue. Text is az/en/ru.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "booking_staff_notes",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_staff_notes_code", columnNames = "code")
)
public class BookingStaffNote {

    public static final String CANCEL = "cancel";
    public static final String NO_SHOW = "no_show";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 16)
    String kind;

    @Column(nullable = false, length = 64)
    String code;

    @Column(name = "title_json", nullable = false, length = 1024)
    String titleJson;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;
}
