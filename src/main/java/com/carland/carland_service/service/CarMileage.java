package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * tr: Yalnız müştərinin öz km yazışı tarixi yeniləyir. Staff, Hyper və booking tamamlanması çağırmır.
 * en: Only the customer's own mileage write moves the date. Staff, Hyper and booking completion do not call this.
 */
public final class CarMileage {

    public static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    private CarMileage() {
    }

    public static void writtenByCustomer(Car car, Long mileage) {
        if (car == null || mileage == null) {
            return;
        }
        car.setMileage(mileage);
        car.setLastMileageUpdateAt(LocalDateTime.now(BAKU));
    }
}
