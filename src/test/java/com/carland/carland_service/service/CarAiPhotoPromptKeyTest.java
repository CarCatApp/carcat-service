package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CarAiPhotoPromptKeyTest {

    @Test
    void samePromptFieldsSameKey() {
        Car a = Car.builder().brand("BMW").model("X5").modelYear(2020).colorId(2L).plateNumber("10-AA-100").build();
        Car b = Car.builder().brand(" bmw ").model("x5").modelYear(2020).colorId(2L).plateNumber("10-aa-100").build();
        assertEquals(CarAiPhotoPromptKey.of(a), CarAiPhotoPromptKey.of(b));
    }

    @Test
    void colorChangeDifferentKey() {
        Car green = Car.builder().brand("BMW").model("X5").modelYear(2020).colorId(2L).plateNumber("10-AA-100").build();
        Car yellow = Car.builder().brand("BMW").model("X5").modelYear(2020).colorId(7L).plateNumber("10-AA-100").build();
        assertNotEquals(CarAiPhotoPromptKey.of(green), CarAiPhotoPromptKey.of(yellow));
    }

    @Test
    void bodyChangeDifferentKey() {
        Car sedan = Car.builder().brand("BMW").model("X5").modelYear(2020).colorId(2L).plateNumber("10-AA-100").bodyType("Sedan").build();
        Car suv = Car.builder().brand("BMW").model("X5").modelYear(2020).colorId(2L).plateNumber("10-AA-100").bodyType("SUV").build();
        assertNotEquals(CarAiPhotoPromptKey.of(sedan), CarAiPhotoPromptKey.of(suv));
    }

    @Test
    void colorNameTextChangeDifferentKey() {
        Car blue = Car.builder().brand("BMW").model("X5").modelYear(2020).colorNameText("göy").plateNumber("10-AA-100").build();
        Car white = Car.builder().brand("BMW").model("X5").modelYear(2020).colorNameText("ağ").plateNumber("10-AA-100").build();
        assertNotEquals(CarAiPhotoPromptKey.of(blue), CarAiPhotoPromptKey.of(white));
    }

    @Test
    void colorIdIgnoresColorNameText() {
        Car a = Car.builder().brand("BMW").model("X5").colorId(2L).colorNameText("göy").plateNumber("10-AA-100").build();
        Car b = Car.builder().brand("BMW").model("X5").colorId(2L).colorNameText("ağ").plateNumber("10-AA-100").build();
        assertEquals(CarAiPhotoPromptKey.of(a), CarAiPhotoPromptKey.of(b));
    }

    @Test
    void mileageIgnored() {
        Car a = Car.builder().brand("BMW").model("X5").colorId(2L).plateNumber("10-AA-100").mileage(1000L).build();
        Car b = Car.builder().brand("BMW").model("X5").colorId(2L).plateNumber("10-AA-100").mileage(90000L).build();
        assertEquals(CarAiPhotoPromptKey.of(a), CarAiPhotoPromptKey.of(b));
    }
}
