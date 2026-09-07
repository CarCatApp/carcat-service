package com.carland.carland_service.test_sima_idda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimaKycErrorCatalogTest {

    @Test
    void code752FollowsLanguage() {
        assertTrue(SimaKycErrorCatalog.message(752, "az").contains("təsdiqləyə"));
        assertTrue(SimaKycErrorCatalog.message(752, "en").toLowerCase().contains("identity"));
        assertTrue(SimaKycErrorCatalog.message(752, "ru").contains("личность"));
    }

    @Test
    void unknownCodeFallsBackTo70000() {
        String az = SimaKycErrorCatalog.message(99999, "az");
        assertEquals(SimaKycErrorCatalog.message(70000, "az"), az);
    }

    @Test
    void scoreGatePicks751Then752() {
        assertEquals(751, SimaKycErrorCatalog.scoreGateCode(0.85, 0.99));
        assertEquals(752, SimaKycErrorCatalog.scoreGateCode(0.99, 0.80));
    }

    @Test
    void limitCopyIncludesConfiguredNumber() {
        assertTrue(SimaKycErrorCatalog.dailyLimit(3, "az").contains("3"));
        assertTrue(SimaKycErrorCatalog.totalLimit(5, "en").contains("5"));
    }
}
