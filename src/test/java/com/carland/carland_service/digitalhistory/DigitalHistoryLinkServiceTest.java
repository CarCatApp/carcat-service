package com.carland.carland_service.digitalhistory;

import com.carland.carland_service.dto.response.DigitalHistoryLinkResponse;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalHistoryLinkServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Mock CustomerRepository customers;
    @Mock CarRepository cars;

    private DigitalHistoryTokens tokens;
    private DigitalHistoryLinkService service;

    @BeforeEach
    void setUp() {
        tokens = new DigitalHistoryTokens(SECRET, "carland", 300, Clock.systemUTC());
        service = new DigitalHistoryLinkService(
                customers, cars, tokens, "https://digital-innovation.agency/carland/server-carland/");
    }

    @Test
    void linkIsThePdfUrlAndLastsFiveMinutes() {
        Customer customer = Customer.builder().userId(9L).build();
        Car car = Car.builder().carId(7L).build();
        when(customers.findByUserIdAndPhoneNumberAndStatus(9L, "0501112233", "ACTIVE")).thenReturn(customer);
        when(cars.findByCarIdAndCustomer(7L, customer)).thenReturn(car);

        DigitalHistoryLinkResponse response = service.create(7L, "0501112233", "9", "az");

        assertEquals(300L, response.getExpiresInSeconds());
        assertTrue(response.getUrl().startsWith(
                "https://digital-innovation.agency/carland/server-carland/api/v1/car/digital-history?jwt="));
        String jwt = response.getUrl().substring(response.getUrl().indexOf("jwt=") + 4);
        assertEquals(7L, tokens.parseCarId(jwt));
    }

    @Test
    void anotherCustomersCarIsHidden() {
        Customer customer = Customer.builder().userId(9L).build();
        when(customers.findByUserIdAndPhoneNumberAndStatus(9L, "050", "ACTIVE")).thenReturn(customer);
        when(cars.findByCarIdAndCustomer(7L, customer)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> service.create(7L, "050", "9", "az"));
    }

    @Test
    void missingCustomerIsRejected() {
        when(customers.findByUserIdAndPhoneNumberAndStatus(9L, "050", "ACTIVE")).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> service.create(7L, "050", "9", "az"));
    }
}
