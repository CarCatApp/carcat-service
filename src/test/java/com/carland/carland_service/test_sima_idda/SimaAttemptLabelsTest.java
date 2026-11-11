package com.carland.carland_service.test_sima_idda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimaAttemptLabelsTest {

    @Test
    void mapsCitizenPassportAndForeignTempPerm() {
        assertEquals("citizen", SimaAttemptLabels.kind("CITIZEN", null));
        assertEquals("passport", SimaAttemptLabels.kind("PASSPORT", null));
        assertEquals("foreign/temp", SimaAttemptLabels.kind("FOREIGN", "TRC"));
        assertEquals("foreign/perm", SimaAttemptLabels.kind("FOREIGN", "PRC"));
        assertEquals("foreign/erp", SimaAttemptLabels.kind("FOREIGN", "ERP"));
        assertEquals("foreign", SimaAttemptLabels.kind("FOREIGN", null));
    }
}
