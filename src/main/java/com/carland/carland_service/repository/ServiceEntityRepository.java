package com.carland.carland_service.repository;

import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: ServiceEntity için JPA repository; bakım şablonlarına bağlı servis tanımlarını sorgular.
 * en: JPA repository for ServiceEntity; queries service definitions tied to maintenance templates.
 */
@Repository
public interface ServiceEntityRepository extends JpaRepository<ServiceEntity, Long> {
    /** tr: Ad, işlem tipi, aralık (km/ay) ve şablona göre servis tanımını bulur. / en: Finds a service definition by name, action type, interval (km/month) and template. */
    ServiceEntity findByServiceNameAndActionTypeAndIntervalKmAndIntervalMonthAndMaintenanceTemplate(String serviceName, String actionType, Long intervalKm, Integer intervalMonth, MaintenanceTemplate template);

    /** tr: Servis adına göre servis tanımını bulur. / en: Finds a service definition by service name. */
    ServiceEntity findByServiceName(String serviceName);

    /** tr: Servis adı ve işlem tipine göre servis tanımını bulur. / en: Finds a service definition by service name and action type. */
    ServiceEntity findByServiceNameAndActionType(String serviceName, String actionType);

    /** tr: Bakım şablonuna bağlı tüm servis tanımlarını listeler. / en: Lists all service definitions of a maintenance template. */
    List<ServiceEntity> findAllByMaintenanceTemplate(MaintenanceTemplate template);
}
