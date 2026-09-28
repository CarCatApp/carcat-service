package com.carland.carland_service.repository;

import com.carland.carland_service.entity.ServiceCategoryPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: Hizmet kategorisi ikonu. PercentagePhotoRepository.findByServiceId ile aynı şekil.
 * en: Service-category icon. Same shape as PercentagePhotoRepository.findByServiceId.
 */
@Repository
public interface ServiceCategoryPhotoRepository extends JpaRepository<ServiceCategoryPhoto, Long> {

    ServiceCategoryPhoto findByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    void deleteByCategoryId(Long categoryId);
}
