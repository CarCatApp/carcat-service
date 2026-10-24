package com.carland.carland_service.idda;

import com.carland.carland_service.entity.Brand;
import com.carland.carland_service.entity.Model;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IddaMarkSplitterTest {

    private final List<Brand> brands = List.of(
            brand(1L, "Honda"),
            brand(2L, "BMW"),
            brand(3L, "BMW Alpina"),
            brand(4L, "Alfa Romeo"),
            brand(5L, "Land Rover")
    );

    private final List<Model> models = List.of(
            model(1L, 1L, "Insight"),
            model(2L, 2L, "520"),
            model(3L, 2L, "520d")
    );

    @Test
    void hondaInsightDropsTypeAndFuelParen() {
        IddaMarkSplit split = IddaMarkSplitter.split(
                "HONDA İNSİGHT(HİBRİD) MİNİK", "MİNİK", brands, models);
        assertEquals("Honda", split.brand());
        assertEquals("Insight", split.model());
    }

    @Test
    void bmwKeepsTrailingLetterInsteadOfGuessingDiesel() {
        IddaMarkSplit split = IddaMarkSplitter.split(
                "BMW 520 D MİNİK", "MİNİK", brands, models);
        assertEquals("BMW", split.brand());
        assertEquals("520 D", split.model());
    }

    @Test
    void longestBrandWins() {
        IddaMarkSplit split = IddaMarkSplitter.split(
                "BMW ALPINA B7 MİNİK", "MİNİK", brands, models);
        assertEquals("BMW Alpina", split.brand());
        assertEquals("B7", split.model());
    }

    @Test
    void twoWordBrandStaysTogether() {
        IddaMarkSplit split = IddaMarkSplitter.split(
                "LAND ROVER RANGE ROVER MİNİK", "MİNİK", brands, models);
        assertEquals("Land Rover", split.brand());
        assertEquals("RANGE ROVER", split.model());
    }

    @Test
    void unknownBrandIsNotCutOnTheFirstSpace() {
        IddaMarkSplit split = IddaMarkSplitter.split(
                "great wall gr333minik", "minik", brands, models);
        assertEquals("great wall gr333", split.brand());
        assertNull(split.model());
    }

    @Test
    void plateKeyIgnoresDashSpaceAndCase() {
        assertEquals("77BU669", IddaPlateKey.of("77-bu-669"));
        assertEquals("77BU669", IddaPlateKey.of("77 BU 669"));
        assertEquals("77BU669", IddaPlateKey.of("77BU669"));
    }

    private static Brand brand(Long id, String name) {
        return Brand.builder().brandId(id).brandName(name).build();
    }

    private static Model model(Long id, Long brandId, String name) {
        return Model.builder().modelId(id).brandId(brandId).modelName(name).build();
    }
}
