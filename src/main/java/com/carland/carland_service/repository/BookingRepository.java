package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByRange_RangeIdAndStatusIn(Long rangeId, Collection<String> statuses);
}
