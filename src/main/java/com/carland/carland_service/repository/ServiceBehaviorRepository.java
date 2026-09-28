package com.carland.carland_service.repository;

import com.carland.carland_service.entity.ServiceBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * tr: Xidmət grupları. Sıra sort_order, sonra id.
 * en: Service groups. Ordered by sort_order, then id.
 */
@Repository
public interface ServiceBehaviorRepository extends JpaRepository<ServiceBehavior, Long> {

    List<ServiceBehavior> findAllByOrderBySortOrderAscIdAsc();

    List<ServiceBehavior> findByActiveTrueOrderBySortOrderAscIdAsc();

    Optional<ServiceBehavior> findByCode(String code);

    boolean existsByCode(String code);
}
