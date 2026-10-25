package com.carland.carland_service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlateNumbersTest {

    @Test
    void formatsTheFourPatterns() {
        assertEquals("00 XX 000", PlateNumbers.format("00-XX-000"));
        assertEquals("00 XX 000", PlateNumbers.format("00 xx 000"));
        assertEquals("00 XX 000", PlateNumbers.format("00XX000"));
        assertEquals("021 D 091", PlateNumbers.format("021-D-091"));
        assertEquals("021 D 091", PlateNumbers.format("021D091"));
        assertEquals("M 591 884", PlateNumbers.format("m591884"));
        assertEquals("99 G 250", PlateNumbers.format("99-G-250"));
        assertEquals("99 G 250", PlateNumbers.format("99G250"));
        assertEquals("99 GA 250", PlateNumbers.format("99GA250"));
    }

    @Test
    void formattedInputIsUnchanged() {
        assertEquals("00 XX 000", PlateNumbers.format(PlateNumbers.format("00-XX-000")));
        assertEquals("021 D 091", PlateNumbers.format(PlateNumbers.format("021-D-091")));
        assertEquals("M 591 884", PlateNumbers.format(PlateNumbers.format("m591884")));
        assertEquals("99 G 250", PlateNumbers.format(PlateNumbers.format("99-G-250")));
        assertEquals("99 GA 250", PlateNumbers.format(PlateNumbers.format("99GA250")));
    }

    @Test
    void nullAndUnknownStayAsIs() {
        assertNull(PlateNumbers.format(null));
        assertEquals("not-a-plate", PlateNumbers.format("not-a-plate"));
        assertEquals("", PlateNumbers.format(""));
        assertEquals("99G2500", PlateNumbers.format("99G2500"));
        assertEquals("1234567", PlateNumbers.format("1234567"));
    }
}
