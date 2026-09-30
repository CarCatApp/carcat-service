package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchCarePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Şubenin dövri qulluq paketleri.
 * en: A branch's routine-care packages.
 */
@Repository
public interface BranchCarePackageRepository extends JpaRepository<BranchCarePackage, Long> {

    List<BranchCarePackage> findByBranch_IdOrderByIdAsc(Long branchId);

    long countByBranch_IdAndActiveTrue(Long branchId);
}
