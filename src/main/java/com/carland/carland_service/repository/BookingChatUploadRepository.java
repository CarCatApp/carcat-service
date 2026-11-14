package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingChatUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingChatUploadRepository extends JpaRepository<BookingChatUpload, Long> {

    Optional<BookingChatUpload> findByPublicId(String publicId);

    Optional<BookingChatUpload> findByBooking_IdAndPublicId(Long bookingId, String publicId);
}
