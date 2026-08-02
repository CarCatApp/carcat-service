package com.carland.carland_service.repository;

import com.carland.carland_service.entity.EngineType;
import com.carland.carland_service.entity.ModelYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: ModelYear entity'si için JPA repository; model yılı sözlük kayıtlarını sorgular.
 * en: JPA repository for the ModelYear entity; queries model year lookup records.
 */
@Repository
public interface ModelYearRepository extends JpaRepository<ModelYear, Long> {


    /** tr: Duruma göre model yıllarını listeler. / en: Lists model years by status. */
    List<ModelYear> findAllByStatus(String status);

    /** tr: Duruma göre model yıllarını azalan sırada listeler. / en: Lists model years by status, ordered descending. */
    List<ModelYear> findAllByStatusOrderByModelYearDesc(String status);

}
