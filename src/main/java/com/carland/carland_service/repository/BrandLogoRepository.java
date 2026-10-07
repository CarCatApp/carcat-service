package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BrandLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandLogoRepository extends JpaRepository<BrandLogo, Long> {

    BrandLogo findByBrandId(Long brandId);

    @Query("select b.brandId from BrandLogo b")
    List<Long> findAllBrandIds();
}
