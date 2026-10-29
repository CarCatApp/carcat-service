package com.carland.carland_service.idda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IddaPlateCheckSpecTest {

    @Test
    void plateNeedleDropsSeparators() {
        assertEquals("77BU669", IddaPlateCheckSpec.plateNeedle("77-bu-669"));
        assertEquals("77BU669", IddaPlateCheckSpec.plateNeedle("77 BU 669"));
        assertNull(IddaPlateCheckSpec.plateNeedle(" - "));
        assertNull(IddaPlateCheckSpec.plateNeedle("  "));
    }

    @Test
    void vinNeedleIsUppercaseWithoutSpaces() {
        assertEquals("19XZE4F54NE012640", IddaPlateCheckSpec.vinNeedle("19xze4f54ne012640"));
        assertEquals("19XZE4F54NE012640", IddaPlateCheckSpec.vinNeedle("19XZE4F54 NE012640"));
        assertNull(IddaPlateCheckSpec.vinNeedle("   "));
    }
}
