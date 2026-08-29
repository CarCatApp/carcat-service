package com.carland.carland_service.repository;

import com.carland.carland_service.entity.PushCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: PushCampaign entity'si için JPA repository.
 * en: JPA repository for PushCampaign.
 */
@Repository
public interface PushCampaignRepository extends JpaRepository<PushCampaign, Long> {
}
