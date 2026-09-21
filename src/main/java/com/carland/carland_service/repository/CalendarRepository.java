package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    Calendar findByDayAndServiceCategoryAndBranch(LocalDate utcDay, String serviceCategory, Branch branch);

    @Query("select distinct c from Calendar c join fetch c.timeRanges join fetch c.branch b join fetch b.partner "
            + "where b.id = :branchId and c.day >= :fromDay and c.day <= :toDay")
    List<Calendar> findByBranchIdAndDayBetween(
            @Param("branchId") Long branchId,
            @Param("fromDay") LocalDate fromDay,
            @Param("toDay") LocalDate toDay);
}
