package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingBranch;
import com.carland.carland_service.entity.BookingPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingBranchRepository extends JpaRepository<BookingBranch, Long> {

    List<BookingBranch> findByPartnerOrderByIdAsc(BookingPartner partner);

    List<BookingBranch> findByPartnerIdOrderByIdAsc(Long partnerId);
}
