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

    @Test
    void promptFixesAsciiAgToSoftG() {
        assertEquals(
                ", painted in the Azerbaijani color \"ağ\" (use the English paint color)",
                CarIddaColor.azerbaijaniClause("Ag"));
        assertEquals(
                ", painted in the Azerbaijani color \"ağ mirvari\" (use the English paint color)",
                CarIddaColor.azerbaijaniClause("Ag mirvari"));
        assertEquals("ağ", CarIddaColor.forPrompt("AG"));
        assertEquals("ağ mirvari", CarIddaColor.forPrompt("AG MİRVARİ"));
        assertEquals("ağ", CarIddaColor.forPrompt("ağ"));
        assertEquals("Ağ mirvari", CarIddaColor.forPrompt("Ağ mirvari"));
    }

    @Test
    void normalizeKeepsIddaAsciiAgUnchanged() {
        Car car = Car.builder().build();
        CarIddaColor.apply(car, null, "Ag");
        assertEquals("Ag", car.getIddaColor());
        CarIddaColor.apply(car, null, "Ag mirvari");
        assertEquals("Ag mirvari", car.getIddaColor());
    }
}
