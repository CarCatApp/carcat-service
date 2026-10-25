package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BannerPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannerPhotoRepository extends JpaRepository<BannerPhoto, Long> {

    BannerPhoto findByBannerId(Long bannerId);
}
