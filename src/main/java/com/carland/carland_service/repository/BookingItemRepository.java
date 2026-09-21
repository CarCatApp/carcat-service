package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    List<BookingItem> findByBooking_IdOrderByIdAsc(Long bookingId);

    List<BookingItem> findByBooking_IdIn(Collection<Long> bookingIds);
}
