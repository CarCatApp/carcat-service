package com.carland.carland_service.dto.booking;

import com.carland.carland_service.util.PlateNumberSerializer;
import com.carland.carland_service.util.PlateNumbers;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Partner hover araç kutusu. Book anı kopyası (bookings.car_*) — create sonrası canlı cars oxunmaz.
 *     Yalnız snap boş köhnə sətirlərdə canlı cars / colors adı.
 * en: Partner hover vehicle box. Booking-time copy (bookings.car_*); live cars is not read after create.
 *     Only legacy rows with an empty snap fall back to live cars / colors-table name.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarResponseForSlotPanel {
    Long carId;
    Long brandId;
    String brand;
    String model;
    @JsonSerialize(using = PlateNumberSerializer.class)
    String plateNumber;
    String vin;
    String bodyType;
    String engineType;
    Integer modelYear;
    Integer engineVolume;
    Long mileage;
    String color;

    /** tr: Görüntü formatı. / en: Display format. */
    public String getPlateNumber() {
        return PlateNumbers.format(plateNumber);
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = PlateNumbers.format(plateNumber);
    }
}
