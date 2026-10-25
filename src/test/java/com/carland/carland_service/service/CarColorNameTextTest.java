package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CarColorNameTextTest {

    @Test
    void textClearsCatalogColor() {
        Car car = Car.builder().colorId(3L).build();
        CarColorNameText.apply(car, null, "  ağ  ");
        assertNull(car.getColorId());
        assertEquals("ağ", car.getColorNameText());
    }

    @Test
    void catalogColorClearsText() {
        Car car = Car.builder().colorNameText("ağ").build();
        CarColorNameText.apply(car, 2L, "göy");
        assertEquals(2L, car.getColorId());
        assertNull(car.getColorNameText());
    }

    @Test
    void clauseUsesTheAzerbaijaniName() {
        assertEquals(
                ", painted in the Azerbaijani color \"tünd göy\" (use the English paint color)",
                CarColorNameText.azerbaijaniClause("tünd göy"));
        assertEquals("", CarColorNameText.azerbaijaniClause("  "));
    }
}
