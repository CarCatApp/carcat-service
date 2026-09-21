package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingMessageRepository extends JpaRepository<BookingMessage, Long> {
}
