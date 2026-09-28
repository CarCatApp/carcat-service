package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchCarePackageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Paket satırları.
 * en: Lines that belong to a care package.
 */
@Repository
public interface BranchCarePackageItemRepository extends JpaRepository<BranchCarePackageItem, Long> {

    List<BranchCarePackageItem> findByCarePackage_IdOrderByIdAsc(Long packageId);
}
