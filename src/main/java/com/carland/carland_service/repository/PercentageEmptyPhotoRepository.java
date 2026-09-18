package com.carland.carland_service.repository;

import com.carland.carland_service.entity.PercentageEmptyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * tr: PercentageEmptyPhoto için JPA repository; tek placeholder satırını okur.
 * en: JPA repository for PercentageEmptyPhoto; reads the single placeholder row.
 */
@Repository
public interface PercentageEmptyPhotoRepository extends JpaRepository<PercentageEmptyPhoto, Long> {

    Optional<PercentageEmptyPhoto> findFirstByOrderByImageIdAsc();
}
