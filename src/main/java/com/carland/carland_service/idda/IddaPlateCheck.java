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
 * tr: Canlı IDDA plaka sorgusunun kaydı. Aylık hak buradan sayılır.
 *     Kayıtlı satır plate check cevabına araç olarak basılmaz. Adres ham JSON'da durur.
 * en: A live IDDA plate check. The monthly cap is counted from this table.
 *     A stored row is not returned as the plate-check car. The address stays in the raw JSON.
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

    /**
     * tr: Code 0, VIN dolu ve kişi kısıtı yok. Aylık hak ayrıca countsTowardLimit.
     * en: Code 0, a VIN, and no person restriction. The monthly cap is countsTowardLimit.
     */
    public boolean isVehicleSuccess() {
        if (statusCode == null || statusCode != 0 || vin == null || vin.isBlank()) {
            return false;
        }
        return statusName == null || !statusName.equalsIgnoreCase("ServiceRestrictedByPerson");
    }
}
