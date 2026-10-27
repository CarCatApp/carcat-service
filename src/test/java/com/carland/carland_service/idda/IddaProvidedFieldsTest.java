package com.carland.carland_service.idda;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IddaProvidedFieldsTest {

    @Test
    void lockedIddaColorReplacesTheCatalogName() {
        List<String> locked = IddaProvidedFields.names("Gümüşü metallik");

        assertTrue(locked.contains("iddaColor"));
        assertEquals("Gümüşü metallik", IddaProvidedFields.listColor(locked, "Gümüşü metallik", "Gümüşü"));
        assertTrue(IddaProvidedFields.showsIddaColor(locked, "Gümüşü metallik"));
    }

    @Test
    void catalogNameStaysWhenIddaSentNoColor() {
        List<String> locked = IddaProvidedFields.names(null);

        assertFalse(locked.contains("iddaColor"));
        assertEquals("Gümüşü", IddaProvidedFields.listColor(locked, null, "Gümüşü"));
        assertFalse(IddaProvidedFields.showsIddaColor(locked, null));
    }
}
