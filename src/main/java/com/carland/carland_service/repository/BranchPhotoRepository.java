package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchPhotoRepository extends JpaRepository<BranchPhoto, Long> {

    BranchPhoto findByBranchId(Long branchId);

    boolean existsByBranchId(Long branchId);
}
