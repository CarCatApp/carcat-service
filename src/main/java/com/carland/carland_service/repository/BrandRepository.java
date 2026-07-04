package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findAllByBrandName(String brand);

    boolean existsByBrandName(String brandName);

    boolean existsByIsnew(String isnew);

    List<Brand> findAllByIsnew(String isnew);

}
