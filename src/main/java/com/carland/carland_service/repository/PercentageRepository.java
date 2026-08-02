package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Percentage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Percentage entity'si için JPA repository; araçların servis bazlı bakım yüzdesi kayıtlarını sorgular.
 * en: JPA repository for the Percentage entity; queries per-service maintenance percentage records of cars.
 */
@Repository
public interface PercentageRepository extends JpaRepository<Percentage, Long> {


    /** tr: Araç id'sine göre yüzde kayıtlarını listeler. / en: Lists percentage records by car id. */
    List<Percentage> findAllByCarId(Long carId);




    /** tr: Servis adı ve araç id'sine göre yüzde kaydını bulur. / en: Finds a percentage record by service name and car id. */
    Percentage findByServiceNameAndCarId(String serviceName, Long carId);

    /** tr: Servis id'si ve araç id'sine göre yüzde kaydını bulur. / en: Finds a percentage record by service id and car id. */
    Percentage findByServiceIdAndCarId(Long serviceId, Long carId);
}
