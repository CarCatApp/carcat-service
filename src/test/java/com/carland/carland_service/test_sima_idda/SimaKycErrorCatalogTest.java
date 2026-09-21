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

    @Test
    void code710SendsUserToSupport() {
        assertTrue(SimaKycErrorCatalog.message(710, "az").contains("dəstək"));
        assertTrue(SimaKycErrorCatalog.message(710, "en").toLowerCase().contains("contact support"));
        assertTrue(SimaKycErrorCatalog.message(710, "ru").contains("службу поддержки"));
    }

    @Test
    void paymentCodesUseExcelCopy() {
        assertEquals("FİN kodunuza bağlı məlumat tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                SimaKycErrorCatalog.message(8004, "az"));
        assertTrue(SimaKycErrorCatalog.message(8006, "en").toLowerCase().contains("payment"));
        assertEquals(SimaKycErrorCatalog.message(70000, "az"), SimaKycErrorCatalog.message(8032, "az"));
        assertTrue(SimaKycErrorCatalog.message(8034, "ru").contains("карта"));
        assertTrue(SimaKycErrorCatalog.message(8013, "en").toLowerCase().contains("contact support"));
    }

    @Test
    void code7530ReportsMultipleFaces() {
        assertTrue(SimaKycErrorCatalog.message(7530, "az").contains("birdən artıq"));
        assertTrue(SimaKycErrorCatalog.message(7530, "en").toLowerCase().contains("more than one face"));
    }
}
