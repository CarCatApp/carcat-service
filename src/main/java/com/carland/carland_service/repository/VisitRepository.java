package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.Car;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /**
     * tr: Rozet ekranı. Servis satırı çekmez. Sıra, rozet listesindeki en yeni ziyaretle aynıdır.
     * en: Badge screen. Does not fetch service lines. Order matches the newest visit used by the badge list.
     */
    List<Visit> findByCar_CarIdOrderByLastServiceDateDescIdDesc(Long carId);

    /** tr: Aracın ziyaretlerini servis satırlarıyla birlikte tarihe göre azalan sırada listeler. / en: Lists a car's visits with service lines, ordered by last service date descending. */
    /** Eager-fetch services; parts loaded via {@link org.hibernate.annotations.BatchSize} (Hibernate cannot join-fetch two List bags). */
    @EntityGraph(attributePaths = {"services"})
    List<Visit> findAllByCarOrderByLastServiceDateDescIdDesc(Car car);

    /** tr: Araca ait Hyper recordId değerlerini döndürür (mükerrer ingest kontrolü). / en: Returns the Hyper recordIds of a car (duplicate ingest check). */
    @Query("SELECT v.hyperRecordId FROM Visit v WHERE v.car = :car")
    Set<Long> findHyperRecordIdsByCar(@Param("car") Car car);

    /**
     * tr: Bu partnerde branch_id eşit olan veya branch_id boş ziyareti bulunan araç id'leri.
     *     Boş branch yalnız partner 1 + şube 10 için çağrılır: Hyper ziyareti şube yazmaz, ekranda şube 10 sayılır.
     * en: Car ids that have a visit for this partner with this branch_id, or with a null branch_id.
     *     The null branch is only queried for partner 1 and branch 10: Hyper stores no branch and the screen treats it as branch 10.
     */
    @Query("""
            SELECT DISTINCT v.car.carId FROM Visit v
            WHERE v.car.carId IN :carIds
              AND v.serviceCenterId = :partnerId
              AND (v.branchId = :branchId OR v.branchId IS NULL)
            """)
    Set<Long> findCarIdsWithPartnerBranchOrNullBranch(
            @Param("carIds") Collection<Long> carIds,
            @Param("partnerId") Long partnerId,
            @Param("branchId") Long branchId);

    /** tr: Araç id'si ve Hyper recordId'ye göre ziyareti servis satırlarıyla birlikte bulur. / en: Finds a visit with its service lines by car id and Hyper recordId. */
    @EntityGraph(attributePaths = {"services"})
    @Query("SELECT v FROM Visit v WHERE v.car.carId = :carId AND v.hyperRecordId = :hyperRecordId")
    Optional<Visit> findWithDetailsByCarIdAndHyperRecordId(@Param("carId") Long carId, @Param("hyperRecordId") Long hyperRecordId);

    /**
     * tr: Bu araçtaki Hyper ziyareti. Partner 1, Hyper record id veya hyper kaynak.
     * en: Hyper visits of this car. Partner 1, a Hyper record id, or a hyper source.
     */
    @Query("""
            SELECT COUNT(v) FROM Visit v
            WHERE v.car.carId = :carId
              AND (v.serviceCenterId = :hyperId
                   OR v.hyperRecordId IS NOT NULL
                   OR lower(coalesce(v.source, '')) IN ('hyper', 'hyperservice'))
            """)
    long countHyperVisits(@Param("carId") Long carId, @Param("hyperId") Long hyperId);

    @Query("""
            SELECT MAX(v.lastServiceDate) FROM Visit v
            WHERE v.car.carId = :carId
              AND (v.serviceCenterId = :hyperId
                   OR v.hyperRecordId IS NOT NULL
                   OR lower(coalesce(v.source, '')) IN ('hyper', 'hyperservice'))
            """)
    java.time.LocalDate findLatestHyperVisitDate(@Param("carId") Long carId, @Param("hyperId") Long hyperId);

    boolean existsByCar_Customer_UserIdAndCreatedAtIsNull(Long userId);

    /**
     * tr: Randevuya bağlı panel ziyareti. Tutar buradaysa komisyon onu kullanır.
     * en: Panel visit tied to the booking. Commission uses it when the amount is here.
     */
    @EntityGraph(attributePaths = {"services"})
    @Query("select v from Visit v where v.bookingId = :bookingId order by v.id")
    List<Visit> findCommissionByBookingId(@Param("bookingId") Long bookingId);

    /**
     * tr: Aynı VIN, aynı servis günü, aynı partner. Panel satırı (booking_id dolu) karışmaz.
     * en: Same VIN, same service day, same partner. A panel row (booking_id set) is left out.
     */
    @EntityGraph(attributePaths = {"services"})
    @Query("""
            select v from Visit v join v.car c
            where upper(trim(c.vin)) = :vin
              and v.lastServiceDate = :day
              and v.serviceCenterId = :partnerId
              and v.hyperRecordId is not null
              and v.bookingId is null
            order by v.id
            """)
    List<Visit> findCommissionHyperVisits(@Param("vin") String vin,
                                          @Param("day") java.time.LocalDate day,
                                          @Param("partnerId") Long partnerId);

    @Query("SELECT MIN(v.createdAt) FROM Visit v WHERE v.car.customer.userId = :userId")
    java.time.LocalDateTime findEarliestCreatedAtByCustomer(@Param("userId") Long userId);
}
