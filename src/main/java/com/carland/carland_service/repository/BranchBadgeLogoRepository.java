package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchBadgeLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchBadgeLogoRepository extends JpaRepository<BranchBadgeLogo, Long> {

    BranchBadgeLogo findByBranchId(Long branchId);
}
