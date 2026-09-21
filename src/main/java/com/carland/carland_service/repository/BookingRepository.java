package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByRange_RangeIdAndStatusIn(Long rangeId, Collection<String> statuses);

    boolean existsByRef(String ref);

    Page<Booking> findByBranch_IdAndStatusOrderByCreatedAtDesc(Long branchId, String status, Pageable pageable);

    Page<Booking> findByBranch_Partner_IdAndStatusOrderByCreatedAtDesc(Long partnerId, String status, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    Page<Booking> findByCustomerUserId(Long customerUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    Page<Booking> findByCustomerUserIdAndStatusIn(Long customerUserId, Collection<String> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    Page<Booking> findByCustomerUserIdAndCarId(Long customerUserId, Long carId, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    Page<Booking> findByCustomerUserIdAndCarIdAndStatusIn(
            Long customerUserId, Long carId, Collection<String> statuses, Pageable pageable);

    @Query("select b.status, count(b) from Booking b where b.customerUserId = :userId group by b.status")
    List<Object[]> countGroupByStatus(@Param("userId") Long userId);

    @Query("select b.status, count(b) from Booking b where b.customerUserId = :userId and b.carId = :carId group by b.status")
    List<Object[]> countGroupByStatusAndCarId(@Param("userId") Long userId, @Param("carId") Long carId);
}

