package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Range;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * tr: Range entity'si için JPA repository; takvim zaman aralıklarını sorgular.
 * en: JPA repository for the Range entity; queries calendar time slots.
 */
@Repository
public interface RangeRepository extends JpaRepository<Range, Long> {
    /** tr: Aralık id'sine göre zaman aralığını bulur. / en: Finds a time slot by range id. */
    Range findByRangeId(Long rangeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Range r join fetch r.calendar c join fetch c.branch b join fetch b.partner where r.rangeId = :id")
    Optional<Range> lockByRangeId(@Param("id") Long id);
}
