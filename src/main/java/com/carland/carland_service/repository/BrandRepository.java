package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Brand entity'si için JPA repository; araç markası sözlük kayıtlarını sorgular.
 * en: JPA repository for the Brand entity; queries car brand lookup records.
 */
@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    /** tr: Marka adına göre markaları listeler. / en: Lists brands by brand name. */
    List<Brand> findAllByBrandName(String brand);

    /** tr: Marka adının kayıtlı olup olmadığını kontrol eder. / en: Checks whether a brand name exists. */
    boolean existsByBrandName(String brandName);

    /** tr: Verilen isnew değerine sahip kayıt olup olmadığını kontrol eder. / en: Checks whether any record exists with the given isnew value. */
    boolean existsByIsnew(String isnew);

    /** tr: isnew değerine göre markaları listeler. / en: Lists brands by isnew value. */
    List<Brand> findAllByIsnew(String isnew);

}
