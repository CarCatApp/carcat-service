package com.carland.carland_service.service.webhook;

import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * tr: Hyper webhook payload'ındaki araç bilgilerini (plaka, kilometre, marka, model, yıl, kasa/motor tipi, motor hacmi) Car entity'sine uygulayan bileşen; yalnızca dolu gelen alanları günceller.
 * en: Component applying vehicle metadata from the Hyper webhook payload (plate, mileage, brand, model, year, body/engine type, engine volume) to the Car entity; only non-empty fields are updated.
 */
@Component
@RequiredArgsConstructor
public class HyperWebhookCarMetadataApplier {

    private final CarRepository carRepository;

    /**
     * tr: Hyper cevabındaki dolu alanları araca kopyalar (motor hacmi litreden cc'ye çevrilir, plaka trim'lenir) ve en az bir alan değiştiyse aracı kaydeder. Exception fırlatmaz.
     * en: Copies the non-empty fields from the Hyper response onto the car (engine volume converted from liters to cc, plate trimmed) and saves the car if at least one field changed. Does not throw.
     */
    public void apply(Car car, HyperVehicleByVinResponse hyper) {
        boolean changed = false;

        if (hyper.getPlate() != null && !hyper.getPlate().isBlank()) {
            car.setPlateNumber(hyper.getPlate().trim());
            changed = true;
        }
        if (hyper.getCurrentMileage() != null) {
            car.setMileage(hyper.getCurrentMileage().longValue());
            changed = true;
        }
        if (hyper.getBrand() != null && !hyper.getBrand().isBlank()) {
            car.setBrand(hyper.getBrand());
            changed = true;
        }
        if (hyper.getModel() != null && !hyper.getModel().isBlank()) {
            car.setModel(hyper.getModel());
            changed = true;
        }
        if (hyper.getYear() != null) {
            car.setModelYear(hyper.getYear());
            changed = true;
        }
        if (hyper.getBodyType() != null && !hyper.getBodyType().isBlank()) {
            car.setBodyType(hyper.getBodyType());
            changed = true;
        }
        if (hyper.getEngineType() != null && !hyper.getEngineType().isBlank()) {
            car.setEngineType(hyper.getEngineType());
            changed = true;
        }
        if (hyper.getEngineVolume() != null) {
            car.setEngineVolume((int) (hyper.getEngineVolume() * 1000));
            changed = true;
        }

        if (changed) {
            carRepository.save(car);
        }
    }
}
