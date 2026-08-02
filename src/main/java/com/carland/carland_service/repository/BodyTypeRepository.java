package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BodyType;
import com.carland.carland_service.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: BodyType entity'si için JPA repository; kasa tipi sözlük kayıtlarını sorgular.
 * en: JPA repository for the BodyType entity; queries body type lookup records.
 */
@Repository
public interface BodyTypeRepository extends JpaRepository<BodyType, Long> {



    /** tr: Duruma göre kasa tiplerini id sırasıyla listeler. / en: Lists body types by status, ordered by id. */
    List<BodyType> findAllByStatusOrderByBodyTypeIdAsc(String status);
}
