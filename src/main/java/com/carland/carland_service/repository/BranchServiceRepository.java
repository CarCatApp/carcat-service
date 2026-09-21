package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchServiceRepository extends JpaRepository<BranchService, Long> {

    boolean existsByBranchIdAndServiceKey(Long branchId, String serviceKey);

    List<BranchService> findByBranchIdAndActiveTrueOrderByIdAsc(Long branchId);
}
