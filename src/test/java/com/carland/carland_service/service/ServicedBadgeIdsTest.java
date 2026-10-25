package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.enums.PartnerId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicedBadgeIdsTest {

    @Test
    void newestFirstKeepsHyperTemporaryBranchAndSkipsBranchlessPartners() {
        Visit panel = Visit.builder().branchId(22L).serviceCenterId(4L).build();
        Visit hyper = Visit.builder().serviceCenterId(PartnerId.HYPER.getId()).build();
        Visit other = Visit.builder().serviceCenterId(2L).build();
        Visit repeat = Visit.builder().branchId(22L).serviceCenterId(4L).build();

        List<Visit> visits = List.of(panel, hyper, other, repeat);

        assertEquals(List.of("4", String.valueOf(PartnerId.HYPER.getId()), "2"), ServicedBadgeIds.partnerIds(visits));
        assertEquals(List.of("22", String.valueOf(ServicedBadgeIds.HYPER_TEMPORARY_BRANCH_ID)),
                ServicedBadgeIds.branchIds(visits));
    }

    @Test
    void applyIfChangedWritesBranchIdsOnce() {
        Car car = Car.builder().servicedPartnerIds(List.of("3")).build();
        Visit visit = Visit.builder().branchId(7L).serviceCenterId(3L).build();

        assertTrue(ServicedBadgeIds.applyIfChanged(car, List.of(visit)));
        assertFalse(ServicedBadgeIds.applyIfChanged(car, List.of(visit)));
        assertEquals(List.of("3"), car.getServicedPartnerIds());
        assertEquals(List.of("7"), car.getServicedBranchIds());
    }
}
