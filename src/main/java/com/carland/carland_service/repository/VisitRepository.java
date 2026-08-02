package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.Car;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * tr: Visit entity'si için JPA repository; partner kaynaklı servis ziyaretlerini sorgular.
 * en: JPA repository for the Visit entity; queries partner-sourced service visits.
 */
@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    /** tr: Araca ait ziyaret olup olmadığını kontrol eder. / en: Checks whether any visit exists for the car. */
    boolean existsByCar(Car car);

    /** tr: Aracın ziyaretlerini servis satırlarıyla birlikte tarihe göre azalan sırada listeler. / en: Lists a car's visits with service lines, ordered by last service date descending. */
    /** Eager-fetch services; parts loaded via {@link org.hibernate.annotations.BatchSize} (Hibernate cannot join-fetch two List bags). */
    @EntityGraph(attributePaths = {"services"})
    List<Visit> findAllByCarOrderByLastServiceDateDescIdDesc(Car car);

    /** tr: Araca ait Hyper recordId değerlerini döndürür (mükerrer ingest kontrolü). / en: Returns the Hyper recordIds of a car (duplicate ingest check). */
    @Query("SELECT v.hyperRecordId FROM Visit v WHERE v.car = :car")
    Set<Long> findHyperRecordIdsByCar(@Param("car") Car car);

    /** tr: Araç id'si ve Hyper recordId'ye göre ziyareti servis satırlarıyla birlikte bulur. / en: Finds a visit with its service lines by car id and Hyper recordId. */
    @EntityGraph(attributePaths = {"services"})
    @Query("SELECT v FROM Visit v WHERE v.car.carId = :carId AND v.hyperRecordId = :hyperRecordId")
    Optional<Visit> findWithDetailsByCarIdAndHyperRecordId(@Param("carId") Long carId, @Param("hyperRecordId") Long hyperRecordId);
}
