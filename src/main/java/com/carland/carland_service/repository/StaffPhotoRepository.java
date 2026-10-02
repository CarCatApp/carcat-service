package com.carland.carland_service.repository;

import com.carland.carland_service.entity.StaffPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffPhotoRepository extends JpaRepository<StaffPhoto, Long> {

    StaffPhoto findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
