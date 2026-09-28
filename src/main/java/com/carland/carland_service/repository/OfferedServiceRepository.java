package com.carland.carland_service.repository;

import com.carland.carland_service.entity.OfferedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Gruba bağlı xidmət satırları.
 * en: Service lines that belong to a group.
 */
@Repository
public interface OfferedServiceRepository extends JpaRepository<OfferedService, Long> {

    List<OfferedService> findAllByOrderBySortOrderAscIdAsc();

    List<OfferedService> findByBehavior_IdAndActiveTrueOrderBySortOrderAscIdAsc(Long behaviorId);
}
