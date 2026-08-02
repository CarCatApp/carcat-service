package com.carland.carland_service.repository;

import com.carland.carland_service.entity.EngineType;
import com.carland.carland_service.entity.MaintenanceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * tr: MaintenanceTemplate entity'si için JPA repository; bakım şablonlarını sorgular.
 * en: JPA repository for the MaintenanceTemplate entity; queries maintenance templates.
 */
@Repository
public interface MaintenanceTemplateRepository extends JpaRepository<MaintenanceTemplate, Long> {


    /** tr: Motor tipine göre bakım şablonunu bulur. / en: Finds the maintenance template by engine type. */
    Optional<MaintenanceTemplate> findByEngineType(EngineType engineType);
}
