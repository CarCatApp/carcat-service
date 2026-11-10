package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.repository.ColorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingVehicleSnapshotTest {

    @Mock ColorRepository colorRepository;

    BookingVehicleSnapshot snapshot;

    @BeforeEach
    void setUp() {
        snapshot = new BookingVehicleSnapshot(colorRepository);
    }

    @Test
    void captureCopiesHoverFieldsAndLocalizedColor() {
        when(colorRepository.findByColorId(3L)).thenReturn(Color.builder()
                .colorId(3L)
                .color("Green")
                .az("Yaşıl")
                .ru("Зелёный")
                .build());
        Booking booking = Booking.builder().build();
        Car car = Car.builder()
                .brand(" Toyota ")
                .model("Camry")
                .colorId(3L)
                .bodyType("Sedan")
                .engineType("Hybrid")
                .modelYear(2019)
                .engineVolume(2500)
                .mileage(20000L)
                .build();

        snapshot.capture(booking, car, "az");

        assertEquals("Toyota", booking.getCarBrand());
        assertEquals("Camry", booking.getCarModel());
        assertEquals("Yaşıl", booking.getCarColor());
        assertEquals("Sedan", booking.getCarBodyType());
        assertEquals("Hybrid", booking.getCarEngineType());
        assertEquals(2019, booking.getCarModelYear());
        assertEquals(2500, booking.getCarEngineVolume());
        assertEquals(20000L, booking.getCarMileage());
    }

    @Test
    void captureUsesIddaColorWhenColorIdMissing() {
        Booking booking = Booking.builder().build();
        Car car = Car.builder().brand("BMW").iddaColor("Qara").mileage(1L).build();

        snapshot.capture(booking, car, "en");

        assertEquals("BMW", booking.getCarBrand());
        assertEquals("Qara", booking.getCarColor());
        assertNull(booking.getCarModel());
    }
}
