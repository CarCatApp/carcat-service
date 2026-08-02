package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BodyType;
import com.carland.carland_service.entity.EngineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: EngineType entity'si için JPA repository; motor tipi sözlük kayıtlarını sorgular.
 * en: JPA repository for the EngineType entity; queries engine type lookup records.
 */
@Repository
public interface EngineTypeRepository extends JpaRepository<EngineType, Long> {



    /** tr: Duruma göre motor tiplerini id sırasıyla listeler. / en: Lists engine types by status, ordered by id. */
    List<EngineType> findAllByStatusOrderByEngineTypeIdAsc(String status);

    /** tr: Motor tipi id'sine göre motor tipini bulur. / en: Finds an engine type by engine type id. */
    EngineType findByEngineTypeId(Long engineTypeId);
}
