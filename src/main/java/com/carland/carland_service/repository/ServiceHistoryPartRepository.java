package com.carland.carland_service.repository;

import com.carland.carland_service.entity.ServiceHistory;
import com.carland.carland_service.entity.ServiceHistoryPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: ServiceHistoryPart entity'si için JPA repository; servis geçmişi kayıtlarına bağlı parçaları sorgular.
 * en: JPA repository for the ServiceHistoryPart entity; queries parts attached to service history records.
 */
@Repository
public interface ServiceHistoryPartRepository extends JpaRepository<ServiceHistoryPart, Long> {
    /** tr: Servis geçmişi kaydına ait tüm parçaları listeler. / en: Lists all parts of a service history record. */
    List<ServiceHistoryPart> findAllByServiceHistory(ServiceHistory serviceHistory);
}
