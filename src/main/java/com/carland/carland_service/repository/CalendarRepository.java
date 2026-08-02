package com.carland.carland_service.repository;

import com.carland.carland_service.entity.AutoService;
import com.carland.carland_service.entity.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * tr: Calendar entity'si için JPA repository; randevu takvimlerini sorgular.
 * en: JPA repository for the Calendar entity; queries appointment calendars.
 */
@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {


    /** tr: Gün, servis kategorisi ve servis merkezine göre takvimi bulur. / en: Finds the calendar by day, service category and auto service. */
    Calendar findByDayAndServiceCategoryAndAutoService(LocalDate utcDay, String serviceCategory, AutoService autoService);
}
