package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingStaffAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingStaffAuditRepository extends JpaRepository<BookingStaffAudit, Long> {

    List<BookingStaffAudit> findTop50ByPartnerIdOrderByCreatedAtDesc(Long partnerId);
}
