package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingCancelReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingCancelReasonRepository extends JpaRepository<BookingCancelReason, Long> {

    boolean existsByCode(String code);

    List<BookingCancelReason> findByActiveTrueOrderBySortOrderAscIdAsc();

    Optional<BookingCancelReason> findByCodeAndActiveTrue(String code);
}
