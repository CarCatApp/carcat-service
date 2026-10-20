package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingPackageLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Book anında kopyalanan paket satırları.
 * en: Package lines snapshotted when the booking is created.
 */
@Repository
public interface BookingPackageLineRepository extends JpaRepository<BookingPackageLine, Long> {

    List<BookingPackageLine> findByBooking_IdOrderByIdAsc(Long bookingId);
}
