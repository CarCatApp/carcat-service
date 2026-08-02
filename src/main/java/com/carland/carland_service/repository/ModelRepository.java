package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Model entity'si için JPA repository; araç modeli sözlük kayıtlarını sorgular.
 * en: JPA repository for the Model entity; queries car model lookup records.
 */
@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {

    /** tr: Marka id'sine göre modelleri listeler. / en: Lists models by brand id. */
    List<Model> findAllByBrandId(Long brandId);

    /** tr: isnew değerine göre modelleri listeler. / en: Lists models by isnew value. */
    List<Model> findAllByIsnew(String isnew);

    /** tr: Marka id'si ve isnew değerine göre modelleri listeler. / en: Lists models by brand id and isnew value. */
    List<Model> findAllByBrandIdAndIsnew(Long brandId, String isnew);

}
