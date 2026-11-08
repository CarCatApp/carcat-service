package com.carland.carland_service.repository;

import com.carland.carland_service.entity.CommissionStatusAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommissionStatusAuditRepository extends JpaRepository<CommissionStatusAudit, Long> {

    List<CommissionStatusAudit> findTop20ByReviewRequiredTrueOrderByIdDesc();
}
