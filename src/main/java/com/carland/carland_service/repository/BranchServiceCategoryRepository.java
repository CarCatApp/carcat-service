package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * tr: Şube bazında kategori aç/kapa satırları.
 * en: Per-branch category on/off rows.
 */
@Repository
public interface BranchServiceCategoryRepository extends JpaRepository<BranchServiceCategory, Long> {

    Optional<BranchServiceCategory> findByBranch_IdAndCategory_Id(Long branchId, Long categoryId);

    List<BranchServiceCategory> findByBranch_Id(Long branchId);
}
