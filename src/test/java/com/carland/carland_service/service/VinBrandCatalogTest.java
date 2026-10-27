package com.carland.carland_service.service;

import com.carland.carland_service.entity.Brand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VinBrandCatalogTest {

    private final List<Brand> brands = List.of(
            brand("Mercedes"),
            brand("Mercedes-Maybach"),
            brand("BMW"),
            brand("Land Rover"),
            brand("Volkswagen")
    );

    @Test
    void mercedesBenzUsesCatalogMercedes() {
        assertEquals("Mercedes", VinBrandCatalog.match("MERCEDES-BENZ", brands));
        assertEquals("Mercedes", VinBrandCatalog.match("mercedes-benz", brands));
    }

    @Test
    void exactNameIgnoresCaseAndKeepsCatalogSpelling() {
        assertEquals("Mercedes", VinBrandCatalog.match("MERCEDES", brands));
        assertEquals("BMW", VinBrandCatalog.match("bmw", brands));
        assertEquals("Land Rover", VinBrandCatalog.match("LAND ROVER", brands));
        assertEquals("Volkswagen", VinBrandCatalog.match("VOLKSWAGEN", brands));
    }

    @Test
    void unknownMakeIsNull() {
        assertNull(VinBrandCatalog.match("SSANGYONG", brands));
        assertNull(VinBrandCatalog.match("GREAT WALL", brands));
        assertNull(VinBrandCatalog.match("   ", brands));
        assertNull(VinBrandCatalog.match(null, brands));
    }

    @Test
    void mercedesBenzIsNullWhenCatalogHasNoMercedes() {
        List<Brand> withoutMercedes = List.of(brand("BMW"), brand("Mercedes-Maybach"));
        assertNull(VinBrandCatalog.match("MERCEDES-BENZ", withoutMercedes));
    }

    @Test
    void maybachStaysOnItsOwnName() {
        assertEquals("Mercedes-Maybach", VinBrandCatalog.match("MERCEDES-MAYBACH", brands));
    }

    private static Brand brand(String name) {
        return Brand.builder().brandName(name).build();
    }
}
