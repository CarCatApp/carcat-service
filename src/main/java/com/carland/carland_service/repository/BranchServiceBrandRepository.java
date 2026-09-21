package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchServiceBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchServiceBrandRepository extends JpaRepository<BranchServiceBrand, Long> {
}
