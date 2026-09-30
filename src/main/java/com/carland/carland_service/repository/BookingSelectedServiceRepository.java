package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingSelectedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSelectedServiceRepository extends JpaRepository<BookingSelectedService, Long> {

    List<BookingSelectedService> findByBooking_IdOrderByIdAsc(Long bookingId);
}
