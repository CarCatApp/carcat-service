package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingMessageRepository extends JpaRepository<BookingMessage, Long> {

    List<BookingMessage> findByBooking_IdOrderByCreatedAtAsc(Long bookingId);

    Optional<BookingMessage> findByBooking_IdAndClientMessageId(Long bookingId, String clientMessageId);

    List<BookingMessage> findByBooking_IdAndClientMessageIdIn(Long bookingId, Collection<String> clientMessageIds);

    List<BookingMessage> findByBooking_IdAndFromRoleNotAndDelivery(
            Long bookingId, String fromRole, String delivery);

    /**
     * tr: Şubenin yazdığı ve müşterinin henüz seen yapmadığı mesajlar. Müşteri ve sistem satırı sayılmaz.
     * en: Branch messages the customer has not marked seen. Customer and system rows are excluded.
     */
    @Query("""
            select count(m)
            from BookingMessage m
            where m.booking.id = :bookingId
              and m.fromRole = 'branch'
              and m.delivery <> 'seen'
            """)
    long countBranchUnread(@Param("bookingId") Long bookingId);

    /**
     * tr: Liste sayfası için aynı sayım, rezervasyon id → adet.
     * en: The same count for a list page, booking id to total.
     */
    @Query("""
            select m.booking.id, count(m)
            from BookingMessage m
            where m.booking.id in :ids
              and m.fromRole = 'branch'
              and m.delivery <> 'seen'
            group by m.booking.id
            """)
    List<Object[]> countBranchUnreadByBookingId(@Param("ids") Collection<Long> ids);

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
