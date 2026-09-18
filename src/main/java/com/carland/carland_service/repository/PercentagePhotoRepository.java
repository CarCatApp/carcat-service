package com.carland.carland_service.repository;

import com.carland.carland_service.entity.PercentagePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: PercentagePhoto için JPA repository; servis kalemi ikonlarını sorgular.
 * en: JPA repository for PercentagePhoto; queries per-service percentage icons.
 */
@Repository
public interface PercentagePhotoRepository extends JpaRepository<PercentagePhoto, Long> {

    PercentagePhoto findByServiceId(Long serviceId);
}
