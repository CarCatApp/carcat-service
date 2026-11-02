package com.carland.carland_service.repository;

import com.carland.carland_service.entity.VinServiceOpen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * tr: VIN açılış bayrağı. Satır varsa liste bir daha kapanmaz.
 * en: VIN open flag. A stored row keeps the branch list open.
 */
public interface VinServiceOpenRepository extends JpaRepository<VinServiceOpen, Long> {

    Optional<VinServiceOpen> findByVin(String vin);

    boolean existsByVin(String vin);
}
