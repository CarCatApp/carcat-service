package com.carland.carland_service.util;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.repository.CarSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AdminCarLabelsTest {

    @Test
    void ownerLineJoinsNameAndPhone() {
        Customer customer = Customer.builder().name("Ali").surname("Veli").phoneNumber("0501112233").build();
        assertEquals("Ali Veli -> 0501112233", AdminCarLabels.ownerLine(customer));
        assertEquals("Ali Veli", AdminCarLabels.ownerName(customer));
        assertEquals("0501112233", AdminCarLabels.ownerPhone(customer));
    }

    @Test
    void missingOwnerIsADash() {
        assertEquals("—", AdminCarLabels.ownerLine(null));
        assertEquals("", AdminCarLabels.ownerName(null));
        assertEquals("", AdminCarLabels.ownerPhone(null));
    }

    @Test
    void blankPathCountsAsVin() {
        Car car = Car.builder().build();
        car.setPath("  ");
        assertEquals("vin", AdminCarLabels.pathOf(car));
        assertEquals("vin", AdminCarLabels.pathOf(null));
        assertEquals("plate", AdminCarLabels.pathOf(Car.builder().path("plate").build()));
    }

    @Test
    void plateNeedleIgnoresSpacesDashesAndDots() {
        assertEquals("10BB110", CarSpec.plateNeedle("10-BB-110"));
        assertEquals("10BB110", CarSpec.plateNeedle("10BB110"));
        assertEquals("10BB110", CarSpec.plateNeedle("10 BB 110"));
        assertEquals("10BB110", CarSpec.plateNeedle("10.bb.110"));
        assertNull(CarSpec.plateNeedle(" - . "));
        assertNull(CarSpec.plateNeedle(null));
    }

    @Test
    void ownerAndPathFiltersIgnoreAllAndUnknown() {
        assertNull(CarSpec.ownerKey(null));
        assertNull(CarSpec.ownerKey("all"));
        assertNull(CarSpec.ownerKey("nope"));
        assertEquals("sahipli", CarSpec.ownerKey("Sahipli"));
        assertEquals("sahipsiz", CarSpec.ownerKey("sahipsiz"));
        assertNull(CarSpec.pathKey("all"));
        assertEquals("vin", CarSpec.pathKey("VIN"));
        assertEquals("plate", CarSpec.pathKey("plate"));
    }
}
