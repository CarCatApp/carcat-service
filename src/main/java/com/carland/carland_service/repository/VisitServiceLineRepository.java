package com.carland.carland_service.repository;

import com.carland.carland_service.dto.response.v2.ServiceHistoryV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitServiceLineRepository extends JpaRepository<ServiceHistoryV2, Long> {

    boolean existsByVisit_Car_CarIdAndServiceCode(Long carId, Integer serviceCode);
}
