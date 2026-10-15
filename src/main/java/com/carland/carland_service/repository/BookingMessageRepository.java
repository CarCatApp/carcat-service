package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface BookingMessageRepository extends JpaRepository<BookingMessage, Long> {

    @Modifying(flushAutomatically = true)
    @Query("delete from BookingMessage m where m.booking.id in :bookingIds")
    int deleteByBookingIds(@Param("bookingIds") Collection<Long> bookingIds);
}
