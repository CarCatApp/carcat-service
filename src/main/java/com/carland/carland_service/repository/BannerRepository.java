package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByStatusOrderByIdDesc(String status);

    List<Banner> findByStatusAndShowingOrderByIdDesc(String status, Boolean showing);
}
