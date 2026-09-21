package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchPackageRepository extends JpaRepository<BranchPackage, Long> {

    boolean existsByBranchIdAndServiceKey(Long branchId, String serviceKey);
}
