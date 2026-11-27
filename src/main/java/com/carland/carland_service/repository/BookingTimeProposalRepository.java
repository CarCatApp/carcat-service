package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingTimeProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingTimeProposalRepository extends JpaRepository<BookingTimeProposal, Long> {

    List<BookingTimeProposal> findByBooking_IdAndStatus(Long bookingId, String status);

    Optional<BookingTimeProposal> findByIdAndBooking_Id(Long id, Long bookingId);
}
