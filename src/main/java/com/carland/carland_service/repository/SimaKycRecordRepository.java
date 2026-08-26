package com.carland.carland_service.repository;

import com.carland.carland_service.entity.SimaKycRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SimaKycRecordRepository extends JpaRepository<SimaKycRecord, Long> {

    Optional<SimaKycRecord> findByIdempotencyKey(String idempotencyKey);
}
