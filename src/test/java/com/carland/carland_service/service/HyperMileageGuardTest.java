package com.carland.carland_service.service;

import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitServiceLine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperMileageGuardTest {

    @Test
    void keepsStoredMileageWhenHyperSendsALowerValue() {
        assertEquals(73_000, HyperMileageGuard.keepIfNotLower(73_000, 66_000));
    }

    @Test
    void acceptsAHigherIncomingMileage() {
        assertEquals(73_000, HyperMileageGuard.keepIfNotLower(66_000, 73_000));
    }

    @Test
    void acceptsIncomingWhenStoredIsBlank() {
        assertEquals(66_000, HyperMileageGuard.keepIfNotLower(null, 66_000));
    }

    @Test
    void stampsReplacedLinesUpToTheKeptMileage() {
        Visit visit = Visit.builder().lastServiceMileage(73_000).build();
        visit.addService(VisitServiceLine.builder().doneKm(66_000).build());
        visit.addService(VisitServiceLine.builder().build());

        HyperMileageGuard.stampLinesKeptAbove(visit, 66_000);

        assertEquals(73_000, visit.getServices().get(0).getDoneKm());
        assertEquals(73_000, visit.getServices().get(1).getDoneKm());
    }

    @Test
    void raisesARolledBackVisitUpToTheCard() {
        Visit visit = Visit.builder().lastServiceMileage(66_000).build();
        VisitServiceLine line = VisitServiceLine.builder().doneKm(66_000).build();
        visit.addService(line);

        assertTrue(HyperMileageGuard.raiseVisitMileage(visit, line, 73_000));
        assertEquals(73_000, visit.getLastServiceMileage());
        assertEquals(73_000, line.getDoneKm());
    }

    @Test
    void doesNotWriteWhenTheVisitAlreadyMatchesTheCard() {
        Visit visit = Visit.builder().lastServiceMileage(73_000).build();
        VisitServiceLine line = VisitServiceLine.builder().build();

        assertFalse(HyperMileageGuard.raiseVisitMileage(visit, line, 73_000));
        assertEquals(73_000, visit.getLastServiceMileage());
    }
}
