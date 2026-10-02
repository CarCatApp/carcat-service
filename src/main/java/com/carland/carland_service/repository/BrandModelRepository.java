package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BrandModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BrandModelRepository extends JpaRepository<BrandModel, Long> {

    List<BrandModel> findByBrandModelService_IdOrderByIdAsc(Long brandModelServiceId);

    List<BrandModel> findByBrandModelService_IdInOrderByIdAsc(Collection<Long> brandModelServiceIds);

    void deleteByBrandModelService_Id(Long brandModelServiceId);
}
