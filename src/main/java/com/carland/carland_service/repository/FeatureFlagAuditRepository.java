package com.carland.carland_service.repository;

import com.carland.carland_service.entity.FeatureFlagAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Feature-flag admin değişiklik geçmişi.
 * en: Feature-flag admin change history.
 */
@Repository
public interface FeatureFlagAuditRepository extends JpaRepository<FeatureFlagAudit, Long> {

    List<FeatureFlagAudit> findTop100ByOrderByCreatedAtDesc();

    List<FeatureFlagAudit> findTop50ByAppVersionOrderByCreatedAtDesc(String appVersion);
}
