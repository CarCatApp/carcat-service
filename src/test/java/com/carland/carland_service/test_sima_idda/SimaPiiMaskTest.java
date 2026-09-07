package com.carland.carland_service.test_sima_idda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SimaPiiMaskTest {

    @Test
    void finFirstAndLast() {
        assertEquals("6*****Q", SimaPiiMask.fin("62HJ5KQ"));
        assertEquals("1*****7", SimaPiiMask.fin("1ABC2D7"));
        assertNull(SimaPiiMask.fin(null));
        assertEquals("**", SimaPiiMask.fin("AB"));
    }

    @Test
    void documentFirstTwoLastTwo() {
        assertEquals("AB*****97", SimaPiiMask.documentNumber("AB0668397"));
    }

    @Test
    void birthDateFullyMasked() {
        assertEquals("****-**-**", SimaPiiMask.birthDate("1990-05-17"));
    }

    @Test
    void nameInitial() {
        assertEquals("A***", SimaPiiMask.name("ARAZ"));
        assertEquals("Ə***", SimaPiiMask.name("ƏZİZ"));
    }

    @Test
    void phoneAzOperatorLastTwo() {
        assertEquals("+994 70 ••• •• 70", SimaPiiMask.phone("+994705757570"));
        assertEquals("+994 50 ••• •• 67", SimaPiiMask.phone("994501234567"));
    }
}
