package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BrandModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandModelRepository extends JpaRepository<BrandModel, Long> {

    List<BrandModel> findByBrandModelService_IdOrderByIdAsc(Long brandModelServiceId);

    void deleteByBrandModelService_Id(Long brandModelServiceId);
}
