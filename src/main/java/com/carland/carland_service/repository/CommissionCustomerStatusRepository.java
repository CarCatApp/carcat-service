package com.carland.carland_service.repository;

import com.carland.carland_service.entity.CommissionCustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommissionCustomerStatusRepository extends JpaRepository<CommissionCustomerStatus, Long> {

    Optional<CommissionCustomerStatus> findByVin(String vin);

    boolean existsByVin(String vin);
}
