package com.carland.carland_service.hyper;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CarByVinTest {

    @Test
    void pickPrefersOwnedNewest() {
        Customer owner = Customer.builder().userId(771L).build();
        Car orphan = Car.builder().carId(1133L).vin("C11-437527").build();
        Car ownedOld = Car.builder().carId(1000L).vin("C11437527").customer(owner).build();
        Car ownedNew = Car.builder().carId(1320L).vin("C11437527").customer(owner).build();

        assertEquals(1320L, CarByVin.pick(List.of(orphan, ownedOld, ownedNew)).getCarId());
        assertEquals(1133L, CarByVin.pick(List.of(orphan)).getCarId());
        assertNull(CarByVin.pick(List.of()));
    }
}
