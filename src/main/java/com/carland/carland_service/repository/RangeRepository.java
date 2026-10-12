package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Range;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * tr: Range entity'si için JPA repository; takvim zaman aralıklarını sorgular.
 * en: JPA repository for the Range entity; queries calendar time slots.
 */
@Repository
public interface RangeRepository extends JpaRepository<Range, Long> {
    /** tr: Aralık id'sine göre zaman aralığını bulur. / en: Finds a time slot by range id. */
    Range findByRangeId(Long rangeId);

    @Query("select count(r) from Range r join r.calendar c join c.branch b "
            + "where b.id = :branchId and c.day = :day and r.carePackage.id = :packageId")
    long countCarePackageOnDay(@Param("branchId") Long branchId, @Param("day") LocalDate day,
                               @Param("packageId") Long packageId);

    @Query("select count(r) from Range r join r.calendar c join c.branch b "
            + "where b.id = :branchId and c.day = :day and r.individualService.id = :serviceId")
    long countIndividualOnDay(@Param("branchId") Long branchId, @Param("day") LocalDate day,
                              @Param("serviceId") Long serviceId);

    @Query("select count(r) from Range r join r.calendar c join c.branch b "
            + "where b.id = :branchId and c.day = :day and r.slotTarget = :slotTarget")
    long countTargetOnDay(@Param("branchId") Long branchId, @Param("day") LocalDate day,
                          @Param("slotTarget") String slotTarget);

    /**
     * Same bookable hour as the month calendar: future start, not hidden, AVAILABLE,
     * and workerCount minus appointments minus occupying bookings is at least one.
     * No end date. Later months count.
     */
    @Query("select distinct b.id from Range r join r.calendar c join c.branch b "
            + "where r.individualService.id = :serviceId and r.slotTarget = :slotTarget "
            + "and r.status = :available and r.start > :now and r.end is not null "
            + "and (r.dayHidden is null or r.dayHidden = false) "
            + "and (c.status is null or c.status <> :hiddenCalendar) "
            + "and coalesce(r.workerCount, 0) > ("
            + "cast((select count(a) from Appointment a where a.range = r) as integer) "
            + "+ cast((select count(bk) from Booking bk where bk.range = r and bk.status in :occupying) as integer))")
    List<Long> findBranchIdsWithBookableIndividualSlot(@Param("serviceId") Long serviceId,
                                                        @Param("slotTarget") String slotTarget,
                                                        @Param("available") String available,
                                                        @Param("now") OffsetDateTime now,
                                                        @Param("hiddenCalendar") String hiddenCalendar,
                                                        @Param("occupying") Collection<String> occupying);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Range r join fetch r.calendar c join fetch c.branch b join fetch b.partner where r.rangeId = :id")
    Optional<Range> lockByRangeId(@Param("id") Long id);
}
