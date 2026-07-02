package com.carland.carland_service.service.webhook;

import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HyperWebhookCarMetadataApplier {

    private final CarRepository carRepository;

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
