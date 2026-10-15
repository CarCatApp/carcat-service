package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingSelectedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BookingSelectedServiceRepository extends JpaRepository<BookingSelectedService, Long> {

    List<BookingSelectedService> findByBooking_IdOrderByIdAsc(Long bookingId);

    @Modifying(flushAutomatically = true)
    @Query("delete from BookingSelectedService s where s.booking.id in :bookingIds")
    int deleteByBookingIds(@Param("bookingIds") Collection<Long> bookingIds);
}
