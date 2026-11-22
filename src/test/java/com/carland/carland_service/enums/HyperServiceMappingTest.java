package com.carland.carland_service.enums;

import com.carland.carland_service.service.technicalprofile.MaintenanceItemNames;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HyperServiceMappingTest {

    @Test
    void newTemplateRowsResolveToTheirOwnEnglishName() {
        assertEquals(MaintenanceItemNames.DIFFERENTIAL_OIL, HyperServiceMapping.DIFFERENTIAL_OIL.getNameEn());
        assertEquals(MaintenanceItemNames.TIMING_BELT, HyperServiceMapping.TIMING_BELT_KIT.getNameEn());
        assertEquals(MaintenanceItemNames.TRANSFER_CASE_OIL, HyperServiceMapping.TRANSFER_CASE_OIL.getNameEn());
        assertEquals(MaintenanceItemNames.TRANSMISSION_AT4, HyperServiceMapping.TRANSMISSION_AT4.getNameEn());
        assertEquals(MaintenanceItemNames.TRANSMISSION_AT6_8, HyperServiceMapping.TRANSMISSION_AT6_8.getNameEn());
        assertEquals(MaintenanceItemNames.TRANSMISSION_CVT_DCT, HyperServiceMapping.TRANSMISSION_CVT_DCT.getNameEn());
        assertEquals(MaintenanceItemNames.TRANSMISSION_MANUAL, HyperServiceMapping.TRANSMISSION_MANUAL.getNameEn());
    }

    @Test
    void olderTimingAndTransmissionCodesStayOnTheShortNames() {
        assertEquals("Timing belt", HyperServiceMapping.toNameEn("TIMING_BELT").orElseThrow());
        assertEquals("Timing belt (kit)", HyperServiceMapping.toNameEn("TIMING_BELT_KIT").orElseThrow());
        assertEquals("Transmission fluid", HyperServiceMapping.toNameEn("TRANSMISSION_FLUID").orElseThrow());
        assertEquals("Automatic transmission fluid & filter (AT4)",
                HyperServiceMapping.toNameEn("TRANSMISSION_AT4").orElseThrow());
    }
}
