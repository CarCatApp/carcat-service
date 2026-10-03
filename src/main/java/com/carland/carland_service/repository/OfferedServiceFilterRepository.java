package com.carland.carland_service.repository;

import com.carland.carland_service.entity.OfferedServiceFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferedServiceFilterRepository extends JpaRepository<OfferedServiceFilter, Long> {

    List<OfferedServiceFilter> findAllByOrderByIdAsc();
}
