package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.CarRequest;
import com.carland.carland_service.entity.Car;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarAiPhotoGenerateLockTest {

    @Test
    void remainingSecondsZeroWhenUnlocked() {
        assertEquals(0L, CarAiPhotoGenerateLock.remainingSeconds(null));
        assertEquals(0L, CarAiPhotoGenerateLock.remainingSeconds(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void remainingSecondsAtLeastOneWhenLocked() {
        LocalDateTime until = LocalDateTime.now().plusSeconds(40);
        long rem = CarAiPhotoGenerateLock.remainingSeconds(until);
        assertTrue(rem >= 1L && rem <= 40L);
        assertTrue(CarAiPhotoGenerateLock.isLocked(until));
    }

    @Test
    void colorChangeIsPromptField() {
        Car car = baseCar();
        CarRequest req = CarRequest.builder().colorId(7L).mileage(50_000L).build();
        assertTrue(CarAiPhotoGenerateLock.changesPromptFields(car, req));
    }

    @Test
    void mileageOnlyIsNotPromptField() {
        Car car = baseCar();
        CarRequest req = CarRequest.builder().colorId(2L).mileage(90_000L).build();
        assertFalse(CarAiPhotoGenerateLock.changesPromptFields(car, req));
    }

    @Test
    void samePromptFieldsAreNotAChange() {
        Car car = baseCar();
        CarRequest req = CarRequest.builder()
                .brand(" bmw ")
                .model("x5")
                .modelYear(2020)
                .colorId(2L)
                .plateNumber("10-aa-100")
                .build();
        assertFalse(CarAiPhotoGenerateLock.changesPromptFields(car, req));
    }

    private static Car baseCar() {
        return Car.builder()
                .brand("BMW")
                .model("X5")
                .modelYear(2020)
                .colorId(2L)
                .plateNumber("10-AA-100")
                .mileage(1000L)
                .build();
    }
}
