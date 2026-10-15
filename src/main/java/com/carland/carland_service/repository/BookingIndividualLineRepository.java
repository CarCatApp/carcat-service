package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingIndividualLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BookingIndividualLineRepository extends JpaRepository<BookingIndividualLine, Long> {

    List<BookingIndividualLine> findByBooking_IdOrderByIdAsc(Long bookingId);

    List<BookingIndividualLine> findByBooking_IdIn(Collection<Long> bookingIds);

    long countByIndividualServiceId(Long individualServiceId);

    @Modifying(flushAutomatically = true)
    @Query("delete from BookingIndividualLine l where l.booking.id in :bookingIds")
    int deleteByBookingIds(@Param("bookingIds") Collection<Long> bookingIds);
}
