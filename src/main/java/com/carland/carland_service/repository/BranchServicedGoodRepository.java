package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchServicedGood;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchServicedGoodRepository extends JpaRepository<BranchServicedGood, Long> {

    List<BranchServicedGood> findByBranch_IdOrderByIdAsc(Long branchId);
}
