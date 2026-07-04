package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {

    List<Model> findAllByBrandId(Long brandId);

    List<Model> findAllByIsnew(String isnew);

    List<Model> findAllByBrandIdAndIsnew(Long brandId, String isnew);

}
