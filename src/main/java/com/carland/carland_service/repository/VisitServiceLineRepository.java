package com.carland.carland_service.repository;

import com.carland.carland_service.entity.VisitServiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: VisitServiceLine entity'si için JPA repository; ziyaretlerdeki servis satırlarını sorgular.
 * en: JPA repository for the VisitServiceLine entity; queries service lines within visits.
 */
@Repository
public interface VisitServiceLineRepository extends JpaRepository<VisitServiceLine, Long> {

    /** tr: Araç id'si ve servis koduna göre satır olup olmadığını kontrol eder. / en: Checks whether a line exists by car id and service code. */
    boolean existsByVisit_Car_CarIdAndServiceCode(Long carId, Integer serviceCode);
}
