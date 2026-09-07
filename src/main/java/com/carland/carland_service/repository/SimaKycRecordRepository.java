package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.SimaKycRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface SimaKycRecordRepository extends JpaRepository<SimaKycRecord, Long> {

    Optional<SimaKycRecord> findByIdempotencyKey(String idempotencyKey);

    long countByCustomerAndChannelIn(Customer customer, Collection<String> channels);

    long countByCustomerAndChannelInAndVerifiedFalseAndCreatedAtGreaterThanEqual(
            Customer customer, Collection<String> channels, LocalDateTime from);
}
