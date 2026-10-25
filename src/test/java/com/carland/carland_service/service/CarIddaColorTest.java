package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CarIddaColorTest {

    @Test
    void textClearsCatalogColor() {
        Car car = Car.builder().colorId(3L).build();
        CarIddaColor.apply(car, null, "  ağ  ");
        assertNull(car.getColorId());
        assertEquals("ağ", car.getIddaColor());
    }

    @Test
    void catalogColorClearsText() {
        Car car = Car.builder().iddaColor("ağ").build();
        CarIddaColor.apply(car, 2L, "göy");
        assertEquals(2L, car.getColorId());
        assertNull(car.getIddaColor());
    }

    @Test
    void clauseUsesTheAzerbaijaniName() {
        assertEquals(
                ", painted in the Azerbaijani color \"tünd göy\" (use the English paint color)",
                CarIddaColor.azerbaijaniClause("tünd göy"));
        assertEquals("", CarIddaColor.azerbaijaniClause("  "));
    }
}
