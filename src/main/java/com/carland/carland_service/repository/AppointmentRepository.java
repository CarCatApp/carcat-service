package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByCustomer_UserIdAndServiceCategoryAndAppointmentDateBetweenAndRange_Calendar_Branch_Id(
            Long customerUserId,
            String serviceCategory,
            OffsetDateTime startOfDay,
            OffsetDateTime endOfDay,
            Long branchId
    );

    @Modifying(flushAutomatically = true)
    @Query("delete from Appointment a where a.range.rangeId in :rangeIds")
    int deleteByRangeIds(@Param("rangeIds") Collection<Long> rangeIds);
}
