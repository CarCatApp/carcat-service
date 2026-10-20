package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

/**
 * tr: Book anında paketin dövri satırının kopyası. percentageServiceId bu arabanın şablonundaki services.id.
 * en: Snapshot of a periodic package line at booking time. percentageServiceId is this car's template services.id.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "booking_package_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_package_lines_booking_svc",
                columnNames = {"booking_id", "offered_service_id"}
        ),
        indexes = @Index(name = "idx_booking_package_lines_booking", columnList = "booking_id")
)
public class BookingPackageLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Booking booking;

    @Column(name = "offered_service_id", nullable = false)
    Long offeredServiceId;

    @Column(name = "title_json", length = 1024)
    String titleJson;

    /** Bu arabanın bakım şablonundaki services.id. Kod boşsa veya şablonda yoksa null. */
    @Column(name = "percentage_service_id")
    Long percentageServiceId;
}
