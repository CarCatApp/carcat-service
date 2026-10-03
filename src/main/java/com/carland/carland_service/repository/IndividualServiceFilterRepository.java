package com.carland.carland_service.repository;

import com.carland.carland_service.entity.IndividualServiceFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Fərdi xidmət filter kataloğu.
 * en: Individual-service filter catalog.
 */
@Repository
public interface IndividualServiceFilterRepository extends JpaRepository<IndividualServiceFilter, Long> {

    List<IndividualServiceFilter> findAllByOrderByIdAsc();
}
