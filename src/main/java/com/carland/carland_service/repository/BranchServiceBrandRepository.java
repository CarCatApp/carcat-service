package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchServiceBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BranchServiceBrandRepository extends JpaRepository<BranchServiceBrand, Long> {

    List<BranchServiceBrand> findByBranchService_IdIn(Collection<Long> serviceIds);
}
