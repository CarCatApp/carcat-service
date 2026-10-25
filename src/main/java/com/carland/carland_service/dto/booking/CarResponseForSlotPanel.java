package com.carland.carland_service.dto.booking;

import com.carland.carland_service.util.PlateNumbers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Slot panelindeki araç kutusu. Değerler cars satırının kendisi.
 *     Rəng, colors cədvəlindən Accept-Language adına görə gəlir.
 * en: Vehicle box on the slot panel. Values are the cars row as stored.
 *     Color is the colors-table name for Accept-Language.
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
}
