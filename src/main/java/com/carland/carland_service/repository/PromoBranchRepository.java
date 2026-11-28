package com.carland.carland_service.repository;

import com.carland.carland_service.entity.PromoBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromoBranchRepository extends JpaRepository<PromoBranch, Long> {

    boolean existsByPromo_IdAndBranchId(Long promoId, Long branchId);
}
