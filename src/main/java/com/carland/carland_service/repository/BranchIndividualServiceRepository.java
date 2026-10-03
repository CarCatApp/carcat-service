package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchIndividualService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * tr: Şube fərdi xidmət fiyatı ve durumu.
 * en: Branch individual-service price and status.
 */
@Repository
public interface BranchIndividualServiceRepository extends JpaRepository<BranchIndividualService, Long> {

    List<BranchIndividualService> findByBranch_Id(Long branchId);

    Optional<BranchIndividualService> findByBranch_IdAndIndividualService_Id(Long branchId, Long individualServiceId);

    void deleteByIndividualService_Id(Long individualServiceId);
}
