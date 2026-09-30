package com.carland.carland_service.repository;

import com.carland.carland_service.entity.PartnerPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * tr: PartnerPhoto entity'si için JPA repository; iş ortağı fotoğraflarını sorgular.
 * en: JPA repository for the PartnerPhoto entity; queries partner photos.
 */
@Repository
public interface PartnerPhotoRepository extends JpaRepository<PartnerPhoto, Long> {

    /** tr: Partner id'sine göre fotoğrafı bulur. / en: Finds the photo by partner id. */
    PartnerPhoto findByPartnerId(Long partnerId);

    boolean existsByPartnerId(Long partnerId);

    @Query("select p.partnerId from PartnerPhoto p where p.partnerId in :ids")
    List<Long> findPartnerIdsByPartnerIdIn(@Param("ids") Collection<Long> ids);
}
