package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Color entity'si için JPA repository; renk sözlük kayıtlarını sorgular.
 * en: JPA repository for the Color entity; queries color lookup records.
 */
@Repository
public interface ColorRepository extends JpaRepository<Color, Long> {

    /** tr: Tüm renkleri id sırasıyla listeler. / en: Lists all colors ordered by id. */
    List<Color> findAllByOrderByColorIdAsc();

    /** tr: Renk id'sine göre rengi bulur. / en: Finds a color by color id. */
    Color findByColorId(Long colorId);


}
