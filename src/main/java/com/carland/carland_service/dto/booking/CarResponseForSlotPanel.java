package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Slot panelindeki araç kutusu. Değerler cars satırının kendisi; çeviri ve uydurma yok.
 * en: Vehicle box on the slot panel. Values are the cars row as stored; no translation and no invented specs.
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
}
