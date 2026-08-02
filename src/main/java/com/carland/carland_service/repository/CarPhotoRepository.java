package com.carland.carland_service.repository;

import com.carland.carland_service.entity.CarPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: CarPhoto entity'si için JPA repository; araç fotoğraflarını sorgular.
 * en: JPA repository for the CarPhoto entity; queries car photos.
 */
@Repository
public interface CarPhotoRepository extends JpaRepository<CarPhoto, Long> {


    /** tr: Araç id'sine göre fotoğrafı bulur. / en: Finds the photo by car id. */
    CarPhoto findByCarId(Long carId);
}







