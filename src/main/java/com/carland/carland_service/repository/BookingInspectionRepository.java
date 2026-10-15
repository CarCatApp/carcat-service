package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingInspectionRepository extends JpaRepository<BookingInspection, Long> {

    Optional<BookingInspection> findByBooking_Id(Long bookingId);

    List<BookingInspection> findByBooking_IdIn(Collection<Long> bookingIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from BookingInspection i where i.booking.id in :bookingIds or i.rangeId in :rangeIds")
    int deleteByBookingIdsOrRangeIds(@Param("bookingIds") Collection<Long> bookingIds,
                                     @Param("rangeIds") Collection<Long> rangeIds);
}
