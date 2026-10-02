package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchGood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchGoodRepository extends JpaRepository<BranchGood, Long> {

    List<BranchGood> findByBranch_IdOrderBySortOrderAscIdAsc(Long branchId);
}
