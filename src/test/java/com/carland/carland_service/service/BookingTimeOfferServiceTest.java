package com.carland.carland_service.service;

import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingTimeOfferServiceTest {

    @Test
    void offerStaysOnTheSameService() {
        IndividualService oil = IndividualService.builder().id(3L).build();
        Range oilSlot = Range.builder().slotTarget("individual").individualService(oil).build();
        Range sameOil = Range.builder()
                .slotTarget("individual")
                .individualService(IndividualService.builder().id(3L).build())
                .build();
        Range brakes = Range.builder()
                .slotTarget("individual")
                .individualService(IndividualService.builder().id(4L).build())
                .build();
        Range repair = Range.builder().slotTarget("repair_inspection").build();
        Range otherRepair = Range.builder().slotTarget("repair_inspection").build();

        assertTrue(BookingTimeOfferService.sameService(oilSlot, sameOil));
        assertFalse(BookingTimeOfferService.sameService(oilSlot, brakes));
        assertFalse(BookingTimeOfferService.sameService(oilSlot, repair));
        assertTrue(BookingTimeOfferService.sameService(repair, otherRepair));
    }
}
