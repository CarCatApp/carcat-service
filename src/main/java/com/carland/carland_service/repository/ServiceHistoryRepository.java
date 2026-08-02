package com.carland.carland_service.repository;


import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.ServiceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * tr: ServiceHistory entity'si için JPA repository; araçların servis geçmişi kayıtlarını sorgular.
 * en: JPA repository for the ServiceHistory entity; queries cars' service history records.
 */
@Repository
public interface ServiceHistoryRepository extends JpaRepository<ServiceHistory, Long> {

    /** tr: Servis adı ve araca göre geçmiş kaydını bulur. / en: Finds a history record by service name and car. */
    ServiceHistory findByServiceNameAndCar(String serviceName, Car car);

    /** tr: Servis adı ve araca göre en güncel geçmiş kaydını bulur. / en: Finds the most recent history record by service name and car. */
    @Query("SELECT sh FROM ServiceHistory sh " +
            "WHERE sh.serviceName = :serviceName AND sh.car = :car " +
            "ORDER BY sh.doneDate DESC NULLS LAST, sh.id DESC")
    Optional<ServiceHistory> findTopByServiceNameAndCarOrderByDoneDateDesc(@Param("serviceName") String serviceName,
                                                                           @Param("car") Car car);

    /** tr: Araca ait tüm geçmiş kayıtlarını listeler. / en: Lists all history records of a car. */
    List<ServiceHistory> findAllByCar(Car car);

    /** tr: Araca ait geçmiş kayıtlarını tarihe göre azalan sırada listeler. / en: Lists a car's history records ordered by done date descending. */
    List<ServiceHistory> findAllByCarOrderByDoneDateDescIdDesc(Car car);

    /** tr: Tüm alanları eşleşen mükerrer kaydı arar (idempotent ekleme kontrolü). / en: Looks up a duplicate record matching all fields (idempotent insert check). */
    Optional<ServiceHistory> findByCarAndServiceNameAndDoneDateAndDoneKmAndDealerAndServiceAmountAndSource(
            Car car,
            String serviceName,
            LocalDate doneDate,
            Integer doneKm,
            String dealer,
            BigDecimal serviceAmount,
            String source
    );
}
