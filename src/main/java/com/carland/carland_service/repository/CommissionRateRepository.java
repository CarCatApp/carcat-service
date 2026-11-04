package com.carland.carland_service.repository;

import com.carland.carland_service.entity.CommissionRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommissionRateRepository extends JpaRepository<CommissionRate, Long> {

    List<CommissionRate> findByPartnerIdAndCustomerStatus(Long partnerId, String customerStatus);

    List<CommissionRate> findTop30ByOrderByIdDesc();
}
