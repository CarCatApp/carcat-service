package com.carland.carland_service.repository;

import com.carland.carland_service.entity.CommissionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CommissionEntryRepository extends JpaRepository<CommissionEntry, Long>, JpaSpecificationExecutor<CommissionEntry> {

    boolean existsByBookingId(Long bookingId);

    List<CommissionEntry> findByVin(String vin);
}
