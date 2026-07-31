package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long> {

    Car findByVin(String vin);

    Optional<Car> findByPlateNumberIgnoreCase(String plateNumber);

    List<Car> findAllByCustomer(Customer customer);

    Page<Car> findAll(Pageable pageable);

    Car findByCarId(Long carId);

    Car findByCarIdAndCustomer(Long carId, Customer customer);

    @Query("SELECT c FROM Car c JOIN FETCH c.customer")
    List<Car> findAllWithCustomer();

    List<Car> findAllByCustomerOrderByCreatedAtDesc(Customer customer);

    Car findByVinAndCustomer(String vin, Customer customer);

    List<Car> findByCustomer_UserId(Long userId);

    Page<Car> findByCustomer_UserId(Long userId, Pageable pageable);

}