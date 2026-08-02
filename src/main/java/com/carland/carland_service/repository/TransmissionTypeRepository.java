package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BodyType;
import com.carland.carland_service.entity.TransmissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: TransmissionType entity'si için JPA repository; vites tipi sözlük kayıtlarını sorgular.
 * en: JPA repository for the TransmissionType entity; queries transmission type lookup records.
 */
@Repository
public interface TransmissionTypeRepository extends JpaRepository<TransmissionType, Long> {


    /** tr: Duruma göre vites tiplerini listeler. / en: Lists transmission types by status. */
    List<TransmissionType> findAllByStatus(String status);
}
