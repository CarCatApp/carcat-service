package com.carland.carland_service.repository;

import com.carland.carland_service.entity.FeatureFlagAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Page<FeatureFlagAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<FeatureFlagAudit> findByFlagNameIgnoreCaseOrderByCreatedAtDesc(String flagName, Pageable pageable);

    @Query("""
            select a from FeatureFlagAudit a
            where a.flagId = :flagId
               or (a.flagId is null and a.pathPattern like concat('%', :name, '%'))
            order by a.createdAt desc
            """)
    Page<FeatureFlagAudit> findForFlag(@Param("flagId") Long flagId, @Param("name") String name, Pageable pageable);
}
