package com.carland.carland_service.dto.booking;

import com.carland.carland_service.util.PlateNumberSerializer;
import com.carland.carland_service.util.PlateNumbers;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Partner hover araç kutusu. Book anı kopyası varsa onu gösterir; yoksa canlı cars.
 *     Rəng snap doludursa o mətn; boşdursa colors cədvəlindən Accept-Language adı.
 * en: Partner hover vehicle box. Prefers the booking-time copy; otherwise live cars.
 *     Color is the frozen snap text when present; otherwise the colors-table Accept-Language name.
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
