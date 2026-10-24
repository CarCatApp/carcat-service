package com.carland.carland_service.idda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.LocalDateTime;

/**
 * tr: Canlı IDDA plaka sorgusunun kaydı. Ay içindeki tekrar ve plaka başına 2 hak buradan okunur.
 *     Adres ham JSON'da durur, API cevabına konmaz. DB'den dönüş yeni satır açmaz.
 * en: A live IDDA plate check. The one-month repeat and the two-calls-per-plate cap read this table.
 *     The address stays in the raw JSON and is not returned by the API. A database replay does not insert a row.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "idda_plate_checks", indexes = {
        @Index(name = "idx_idda_plate_check_key_time", columnList = "plate_key, queried_at")
})
public class IddaPlateCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String requestIdentifier;
    String statusName;
    Integer statusCode;
    @Column(length = 500)
    String statusMessage;

    /** tr: Limit anahtarı. / en: Limit key. */
    @Column(nullable = false, length = 32)
    String plateKey;

    String plateNumber;
    String vin;
    @Column(length = 500)
    String rawMark;
    String brand;
    String model;
    Integer modelYear;
    String colorRaw;
    Integer engineCapacity;
    String engineNumber;
    Integer vehicleTypeId;
    String vehicleTypeDescription;
    String certificateNumber;
    String certificateGivenDate;
    String ownerName;
    String ownerSurname;
    String ownerPatronymic;

    @Column(columnDefinition = "text")
    String rawJson;

    Long querierUserId;
    String querierName;
    String querierSurname;

    @Column(nullable = false)
    LocalDateTime queriedAt;

    /**
     * tr: Yalnız Code 0 ve VIN'i dolu sorgu aylık hakka ve müşteri önbelleğine girer.
     * en: Only a Code 0 query that has a VIN counts toward the monthly cap and the customer cache.
     */
    @Column(nullable = false)
    boolean countsTowardLimit;
}
