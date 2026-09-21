package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingCancelReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingCancelReasonRepository extends JpaRepository<BookingCancelReason, Long> {

    boolean existsByCode(String code);
}
