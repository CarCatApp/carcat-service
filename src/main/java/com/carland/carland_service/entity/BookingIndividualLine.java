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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * tr: Book'a seçilen fərdi xidmət. Ad ve manat fiyatları katalog kopyasıdır.
 * en: An individual service picked on a booking. Name and manat prices are a catalog copy.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "booking_individual_lines")
public class BookingIndividualLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Booking booking;

    @Column(name = "individual_service_id", nullable = false)
    Long individualServiceId;

    @Column(length = 8)
    String code;

    @Column(name = "title_json", length = 1024)
    String titleJson;

    @Column(name = "price_simple")
    Integer priceSimple;

    @Column(name = "price_medium")
    Integer priceMedium;

    @Column(name = "price_complex")
    Integer priceComplex;
}
