package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.SimaKycRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SimaKycRecordRepository extends JpaRepository<SimaKycRecord, Long> {

    Optional<SimaKycRecord> findByIdempotencyKey(String idempotencyKey);

    long countByCustomerAndChannelIn(Customer customer, Collection<String> channels);

    long countByCustomerAndChannelInAndVerifiedFalseAndCreatedAtGreaterThanEqual(
            Customer customer, Collection<String> channels, LocalDateTime from);

    long countByCustomerAndChannelInAndCreatedAtAfter(
            Customer customer, Collection<String> channels, LocalDateTime after);

    long countByCustomerAndChannelInAndVerifiedFalseAndCreatedAtAfter(
            Customer customer, Collection<String> channels, LocalDateTime after);

    List<SimaKycRecord> findByCustomerAndChannelInOrderByCreatedAtAsc(
            Customer customer, Collection<String> channels);

    @Query("SELECT r FROM SimaKycRecord r JOIN FETCH r.customer ORDER BY r.id ASC")
    List<SimaKycRecord> findAllWithCustomerOrderByIdAsc();
}
