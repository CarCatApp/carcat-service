package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingStaffRepository extends JpaRepository<BookingStaff, Long> {

    @Query("SELECT s FROM BookingStaff s JOIN FETCH s.partner LEFT JOIN FETCH s.branch WHERE s.userId = :userId")
    List<BookingStaff> findByUserId(@Param("userId") Long userId);

    List<BookingStaff> findByPartnerIdOrderByIdAsc(Long partnerId);

    @Query("SELECT s FROM BookingStaff s LEFT JOIN FETCH s.branch WHERE s.partner.id = :partnerId ORDER BY s.id ASC")
    List<BookingStaff> findByPartnerIdWithBranch(@Param("partnerId") Long partnerId);

    List<BookingStaff> findByBranchId(Long branchId);

    Optional<BookingStaff> findByPartnerIdAndUserIdAndBranchIsNull(Long partnerId, Long userId);

    boolean existsByPartnerIdAndBranchIsNull(Long partnerId);

    boolean existsByUserIdAndPartnerIdNot(Long userId, Long partnerId);

    boolean existsByUserId(Long userId);

    boolean existsByEmailIgnoreCase(String email);
}
