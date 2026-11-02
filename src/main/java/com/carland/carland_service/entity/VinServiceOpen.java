package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.OffsetDateTime;

/**
 * tr: VIN bir kez rakip servislere açıldıysa kayıt burada kalır. Yeni Hyper ziyareti bunu silmez.
 * en: Once a VIN is opened to other services the row stays. A later Hyper visit does not remove it.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "vin_service_opens", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vin_service_opens_vin", columnNames = "vin")
})
public class VinServiceOpen {

    public static final String PASSIVE_YEAR = "PASSIVE_YEAR";
    public static final String KARKAT_BOOKING = "KARKAT_BOOKING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Büyük harf, boşluksuz VIN. */
    @Column(nullable = false, length = 64)
    String vin;

    @Column(nullable = false, length = 32)
    String reason;

    @Column(name = "opened_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime openedAt;

    @Column(name = "car_id")
    Long carId;
}
