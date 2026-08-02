package com.carland.carland_service.repository;

import com.carland.carland_service.entity.PartnerPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: PartnerPhoto entity'si için JPA repository; iş ortağı fotoğraflarını sorgular.
 * en: JPA repository for the PartnerPhoto entity; queries partner photos.
 */
@Repository
public interface PartnerPhotoRepository extends JpaRepository<PartnerPhoto, Long> {

    /** tr: Partner id'sine göre fotoğrafı bulur. / en: Finds the photo by partner id. */
    PartnerPhoto findByPartnerId(Long partnerId);
}
