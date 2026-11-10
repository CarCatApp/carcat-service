package com.carland.carland_service.hyper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperVinTest {

    @Test
    void forHyperApiPadsShortAndDropsDash() {
        assertEquals("C1143752700000000", HyperVin.forHyperApi("C11437527"));
        assertEquals("C1143752700000000", HyperVin.forHyperApi("C-11437527"));
        assertEquals("C1143752700000000", HyperVin.forHyperApi("C11-437527"));
        assertEquals("C1143752700000000", HyperVin.forHyperApi("c11-437527"));
    }

    @Test
    void forHyperApiLeavesSeventeenAlone() {
        assertEquals("C1143752700000000", HyperVin.forHyperApi("C1143752700000000"));
        assertEquals("JTJGB7CX2R4121777", HyperVin.forHyperApi("JTJGB7CX2R4121777"));
    }

    @Test
    void vinKeyUnifiesDashPadAndCompact() {
        assertEquals("C11437527", HyperVin.vinKey("C11437527"));
        assertEquals("C11437527", HyperVin.vinKey("C11-437527"));
        assertEquals("C11437527", HyperVin.vinKey("C-11437527"));
        assertEquals("C11437527", HyperVin.vinKey("C1143752700000000"));
    }

    @Test
    void forDbLookupKeepsNormalSeventeen() {
        assertEquals("JTJGB7CX2R4121777", HyperVin.forDbLookup("JTJGB7CX2R4121777"));
        assertEquals("C1143752700000001", HyperVin.forDbLookup("C1143752700000001"));
    }

    @Test
    void hyperQueryableAfterPad() {
        assertTrue(HyperVin.hyperQueryable("C11-437527"));
        assertTrue(HyperVin.hyperQueryable("C11437527"));
        assertTrue(HyperVin.hyperQueryable("JTJGB7CX2R4121777"));
        assertFalse(HyperVin.hyperQueryable(null));
        assertFalse(HyperVin.hyperQueryable("  "));
    }

    @Test
    void nullAndBlank() {
        assertNull(HyperVin.forHyperApi(null));
        assertNull(HyperVin.forHyperApi("  "));
        assertNull(HyperVin.forDbLookup(null));
        assertNull(HyperVin.compact("---"));
        assertNull(HyperVin.vinKey(null));
    }
}
