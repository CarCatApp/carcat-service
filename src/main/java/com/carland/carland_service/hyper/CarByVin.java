package com.carland.carland_service.hyper;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * tr: VIN ile araç bulma. Tire / Hyper 8-sıfır pad farkını {@link HyperVin#vinKey} ile yok sayar.
 *     Birden fazla satırda (ör. {@code C11-437527} ve {@code C11437527}) sahipli olanı, yoksa en yeni id'yi seçer.
 * en: Finds cars by VIN. Ignores dash / Hyper 8-zero pad via {@link HyperVin#vinKey}.
 *     When several rows match (e.g. {@code C11-437527} and {@code C11437527}), prefers an owned car, else newest id.
 */
@Component
@RequiredArgsConstructor
public class CarByVin {

    private static final Comparator<Car> NEWEST_ID = Comparator
            .comparing(Car::getCarId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final CarRepository carRepository;

    public List<Car> findAll(String vin) {
        String key = HyperVin.vinKey(vin);
        if (key == null) {
            return List.of();
        }
        return carRepository.findAllByVinKey(key);
    }

    public Car findOne(String vin) {
        return pick(findAll(vin));
    }

    public Car findForCustomer(String vin, Customer customer) {
        if (customer == null || customer.getUserId() == null) {
            return null;
        }
        String key = HyperVin.vinKey(vin);
        if (key == null) {
            return null;
        }
        List<Car> matches = carRepository.findAllByVinKeyAndCustomerId(key, customer.getUserId());
        return pick(matches);
    }

    static Car pick(List<Car> cars) {
        if (cars == null || cars.isEmpty()) {
            return null;
        }
        return cars.stream()
                .filter(car -> car.getCustomer() != null)
                .max(NEWEST_ID)
                .orElseGet(() -> cars.stream().max(NEWEST_ID).orElse(null));
    }
}
