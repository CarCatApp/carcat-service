package com.carland.carland_service.repository;

import com.carland.carland_service.entity.IndividualService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * tr: Ortak fərdi xidmət kataloqu.
 * en: Shared individual-service catalog.
 */
@Repository
public interface IndividualServiceRepository extends JpaRepository<IndividualService, Long> {

    Optional<IndividualService> findByCode(String code);

    List<IndividualService> findAllByOrderBySortOrderAscIdAsc();

    List<IndividualService> findByActiveTrueOrderBySortOrderAscIdAsc();
}
