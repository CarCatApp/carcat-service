package com.carland.carland_service.repository;

import com.carland.carland_service.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * tr: Ortak hizmet kategorileri. Sıra sort_order, sonra id.
 * en: Shared service categories. Ordered by sort_order, then id.
 */
@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findAllByOrderBySortOrderAscIdAsc();

    List<ServiceCategory> findByActiveTrueOrderBySortOrderAscIdAsc();

    Optional<ServiceCategory> findByCode(String code);

    boolean existsByCode(String code);
}
