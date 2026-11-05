package com.carland.carland_service.service;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicedBranchesServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CarRepository carRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private BranchRepository branchRepository;
    @InjectMocks
    private ServicedBranchesService service;

    @Test
    void nullColumnReadsVisitOrderAndDoesNotRequireAStoredList() {
        Customer customer = customer();
        Car car = Car.builder().carId(1842L).customer(customer).build();
        Visit panel = Visit.builder()
                .id(8L)
                .branchId(22L)
                .serviceCenterId(4L)
                .lastServiceDate(LocalDate.of(2025, 11, 12))
                .createdAt(LocalDateTime.of(2025, 11, 12, 13, 38))
                .build();
        when(customerRepository.findByUserId(5L)).thenReturn(customer);
        when(carRepository.findByCarIdAndCustomer(1842L, customer)).thenReturn(car);
        when(visitRepository.findByCar_CarIdOrderByLastServiceDateDescIdDesc(1842L)).thenReturn(List.of(panel));
        when(branchRepository.findAllById(List.of(22L))).thenReturn(List.of(
                Branch.builder().id(22L).name("Panel").lat(40.1).lng(49.8).build()));

        var response = service.list(1842L, "5", "az");

        assertEquals(1, response.getBranches().size());
        assertEquals(22L, response.getBranches().get(0).getBranchId());
        assertEquals("12 Noyabr 2025 13:38", response.getBranches().get(0).getLastServiceAt());
    }

    @Test
    void emptyStoredListSkipsVisits() {
        Customer customer = customer();
        Car car = Car.builder().carId(1842L).customer(customer).servicedBranchIds(List.of()).build();
        when(customerRepository.findByUserId(5L)).thenReturn(customer);
        when(carRepository.findByCarIdAndCustomer(1842L, customer)).thenReturn(car);

        var response = service.list(1842L, "5", "az");

        assertTrue(response.getBranches().isEmpty());
        verify(visitRepository, never()).findByCar_CarIdOrderByLastServiceDateDescIdDesc(1842L);
    }

    @Test
    void anotherCustomersCarIsNotFound() {
        Customer customer = customer();
        when(customerRepository.findByUserId(5L)).thenReturn(customer);
        when(carRepository.findByCarIdAndCustomer(1842L, customer)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> service.list(1842L, "5", "az"));
    }

    private static Customer customer() {
        Customer customer = new Customer();
        customer.setUserId(5L);
        customer.setStatus(UserStatus.ACTIVE.name());
        return customer;
    }
}
