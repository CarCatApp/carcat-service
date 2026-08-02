package com.carland.carland_service.repository;

import com.carland.carland_service.entity.PartnerBadgeLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: PartnerBadgeLogo entity'si için JPA repository; iş ortağı rozet logolarını sorgular.
 * en: JPA repository for the PartnerBadgeLogo entity; queries partner badge logos.
 */
@Repository
public interface PartnerBadgeLogoRepository extends JpaRepository<PartnerBadgeLogo, Long> {

    /** tr: Partner id'sine göre rozet logosunu bulur. / en: Finds the badge logo by partner id. */
    PartnerBadgeLogo findByPartnerId(Long partnerId);
}
