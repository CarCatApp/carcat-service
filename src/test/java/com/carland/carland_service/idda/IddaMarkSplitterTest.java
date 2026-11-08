package com.carland.carland_service.idda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IddaMarkSplitterTest {

    @Test
    void firstWordIsBrandAndTheRestIsModel() {
        IddaMarkSplit split = IddaMarkSplitter.split("LİXİANG L7 PRO PLUG-İN HİBRİD", null);
        assertEquals("LİXİANG", split.brand());
        assertEquals("L7 PRO PLUG-İN HİBRİD", split.model());
    }

    @Test
    void hondaDropsTypeAndFuelParen() {
        IddaMarkSplit split = IddaMarkSplitter.split("HONDA İNSİGHT(HİBRİD) MİNİK", "MİNİK");
        assertEquals("HONDA", split.brand());
        assertEquals("İNSİGHT", split.model());
    }

    @Test
    void bmwAlpinaIsNotATwoWordBrand() {
        IddaMarkSplit split = IddaMarkSplitter.split("BMW ALPINA B7 MİNİK", "MİNİK");
        assertEquals("BMW", split.brand());
        assertEquals("ALPINA B7", split.model());
    }

    @Test
    void bmwKeepsTrailingLetter() {
        IddaMarkSplit split = IddaMarkSplitter.split("BMW 520 D MİNİK", "MİNİK");
        assertEquals("BMW", split.brand());
        assertEquals("520 D", split.model());
    }

    @Test
    void gwmTankUsesTheFirstWord() {
        IddaMarkSplit split = IddaMarkSplitter.split("GWM TANK 300 (BENZIN) MİNİK", "MİNİK");
        assertEquals("GWM", split.brand());
        assertEquals("TANK 300", split.model());
    }

    @Test
    void listedTwoWordNamesAreTheBrand() {
        String[][] rows = {
                {"ASTON MARTIN DBX MİNİK", "MİNİK", "Aston Martin", "DBX"},
                {"LAND ROVER RANGE ROVER MİNİK", "MİNİK", "Land Rover", "RANGE ROVER"},
                {"ROLLS-ROYCE GHOST MİNİK", "MİNİK", "Rolls-Royce", "GHOST"},
                {"ROLLS ROYCE GHOST", null, "Rolls-Royce", "GHOST"},
                {"ALFA ROMEO GIULIA", null, "Alfa Romeo", "GIULIA"},
                {"MERCEDES-BENZ C 180", null, "Mercedes-Benz", "C 180"},
                {"MERCEDES BENZ C 180", null, "Mercedes-Benz", "C 180"},
                {"MINI COOPER S", null, "Mini Cooper", "S"},
                {"GREAT WALL GR333", null, "Great Wall", "GR333"},
                {"TATA MOTORS NEXON", null, "Tata Motors", "NEXON"},
                {"MORGAN MOTOR PLUS 4", null, "Morgan Motor", "PLUS 4"},
                {"LUCID MOTORS AIR", null, "Lucid Motors", "AIR"},
                {"FISKER AUTOMOTIVE OCEAN", null, "Fisker Automotive", "OCEAN"},
                {"GENESIS MOTOR G70", null, "Genesis Motor", "G70"},
                {"DE TOMASO PANTERA", null, "De Tomaso", "PANTERA"},
                {"POLESTAR AUTOMOTIVE 2", null, "Polestar Automotive", "2"},
                {"land rover range rover", null, "Land Rover", "range rover"}
        };
        for (String[] row : rows) {
            IddaMarkSplit split = IddaMarkSplitter.split(row[0], row[1]);
            assertEquals(row[2], split.brand(), row[0]);
            assertEquals(row[3], split.model(), row[0]);
        }
    }

    @Test
    void miniWithoutCooperStaysOneWord() {
        IddaMarkSplit split = IddaMarkSplitter.split("MINI CLUBMAN", null);
        assertEquals("MINI", split.brand());
        assertEquals("CLUBMAN", split.model());
    }

    @Test
    void twoWordBrandRequiresAWordBoundary() {
        IddaMarkSplit split = IddaMarkSplitter.split("LAND ROVERSPORT MİNİK", "MİNİK");
        assertEquals("LAND", split.brand());
        assertEquals("ROVERSPORT", split.model());
    }

    @Test
    void brandAloneLeavesModelEmpty() {
        IddaMarkSplit split = IddaMarkSplitter.split("LAND ROVER MİNİK", "MİNİK");
        assertEquals("Land Rover", split.brand());
        assertNull(split.model());
    }

    @Test
    void plateKeyIgnoresDashSpaceAndCase() {
        assertEquals("77BU669", IddaPlateKey.of("77-bu-669"));
        assertEquals("77BU669", IddaPlateKey.of("77 BU 669"));
        assertEquals("77BU669", IddaPlateKey.of("77BU669"));
    }
}
