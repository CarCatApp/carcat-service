package com.carland.carland_service.repository;

import com.carland.carland_service.entity.PercentagePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * tr: PercentagePhoto için JPA repository; servis kalemi ikonlarını sorgular.
 * en: JPA repository for PercentagePhoto; queries per-service percentage icons.
 */
@Repository
public interface PercentagePhotoRepository extends JpaRepository<PercentagePhoto, Long> {

    PercentagePhoto findByServiceId(Long serviceId);

    /** tr: Şablondan düşen kalemin ikonunu siler. / en: Deletes the icon of a service row removed from a template. */
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("delete from PercentagePhoto p where p.serviceId = :serviceId")
    void deleteByServiceId(@Param("serviceId") Long serviceId);
}
