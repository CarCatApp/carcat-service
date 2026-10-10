package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BrandModelService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandModelServiceRepository extends JpaRepository<BrandModelService, Long> {

    List<BrandModelService> findAllByOrderBySortOrderAscIdAsc();
}
