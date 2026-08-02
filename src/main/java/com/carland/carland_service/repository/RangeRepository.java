package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Range;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: Range entity'si için JPA repository; takvim zaman aralıklarını sorgular.
 * en: JPA repository for the Range entity; queries calendar time slots.
 */
@Repository
public interface RangeRepository extends JpaRepository<Range, Long> {
    /** tr: Aralık id'sine göre zaman aralığını bulur. / en: Finds a time slot by range id. */
    Range findByRangeId(Long rangeId);


}
