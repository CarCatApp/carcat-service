package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingMessageRepository extends JpaRepository<BookingMessage, Long> {

    List<BookingMessage> findByBooking_IdOrderByCreatedAtAsc(Long bookingId);

    Optional<BookingMessage> findByBooking_IdAndClientMessageId(Long bookingId, String clientMessageId);

    @Query("""
            select m from BookingMessage m
            where m.booking.id = :bookingId
            order by m.createdAt desc, m.id desc
            """)
    List<BookingMessage> findLatest(@Param("bookingId") Long bookingId, Pageable pageable);

    @Query("""
            select m from BookingMessage m
            where m.booking.id = :bookingId
              and (m.createdAt < :createdAt
                   or (m.createdAt = :createdAt and m.id < :id))
            order by m.createdAt desc, m.id desc
            """)
    List<BookingMessage> findOlderThan(
            @Param("bookingId") Long bookingId,
            @Param("createdAt") OffsetDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);
}
