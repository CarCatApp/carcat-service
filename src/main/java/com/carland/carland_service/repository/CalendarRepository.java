package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    Calendar findByDayAndServiceCategoryAndBranch(LocalDate utcDay, String serviceCategory, Branch branch);
}
