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
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByRange_RangeIdAndStatusIn(Long rangeId, Collection<String> statuses);

    long countByRange_RangeIdAndStatusInAndIdNot(Long rangeId, Collection<String> statuses, Long id);

    boolean existsByRef(String ref);

    boolean existsByCustomerUserIdAndBranch_Id(Long customerUserId, Long branchId);

    boolean existsByCustomerUserIdAndBranch_Partner_Id(Long customerUserId, Long partnerId);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    Optional<Booking> findByRef(String ref);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    @Override
    Optional<Booking> findById(Long id);

    Page<Booking> findByBranch_IdAndStatusOrderByCreatedAtDesc(Long branchId, String status, Pageable pageable);

    Page<Booking> findByBranch_IdAndStatusInOrderByCreatedAtDesc(
            Long branchId, Collection<String> statuses, Pageable pageable);

    Page<Booking> findByBranch_Partner_IdAndStatusOrderByCreatedAtDesc(Long partnerId, String status, Pageable pageable);

    Page<Booking> findByBranch_Partner_IdAndStatusInOrderByCreatedAtDesc(
            Long partnerId, Collection<String> statuses, Pageable pageable);

    List<Booking> findByRange_RangeIdAndStatus(Long rangeId, String status);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    Page<Booking> findByCustomerUserId(Long customerUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    Page<Booking> findByCustomerUserIdAndStatusIn(Long customerUserId, Collection<String> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    Page<Booking> findByCustomerUserIdAndCarId(Long customerUserId, Long carId, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    Page<Booking> findByCustomerUserIdAndCarIdAndStatusIn(
            Long customerUserId, Long carId, Collection<String> statuses, Pageable pageable);

    /**
     * tr: Şube başına son kayıt: created_at en büyük, eşitlikte id en büyük. Sayfa bu küme üzerinde, created_at azalan.
     * en: Latest row per branch: max created_at, then max id. Page that set, created_at descending.
     */
    @Query(
            value = """
                    select b.id from Booking b
                    where b.customerUserId = :userId
                      and b.createdAt = (
                        select max(b2.createdAt) from Booking b2
                        where b2.customerUserId = :userId
                          and b2.branch.id = b.branch.id
                      )
                      and b.id = (
                        select max(b3.id) from Booking b3
                        where b3.customerUserId = :userId
                          and b3.branch.id = b.branch.id
                          and b3.createdAt = b.createdAt
                      )
                    order by b.createdAt desc, b.id desc
                    """,
            countQuery = """
                    select count(distinct b.branch.id) from Booking b
                    where b.customerUserId = :userId
                    """
    )
    Page<Long> findLatestIdPerBranch(@Param("userId") Long userId, Pageable pageable);

    @Query(
            value = """
                    select b.id from Booking b
                    where b.customerUserId = :userId
                      and b.carId = :carId
                      and b.createdAt = (
                        select max(b2.createdAt) from Booking b2
                        where b2.customerUserId = :userId
                          and b2.branch.id = b.branch.id
                          and b2.carId = :carId
                      )
                      and b.id = (
                        select max(b3.id) from Booking b3
                        where b3.customerUserId = :userId
                          and b3.branch.id = b.branch.id
                          and b3.carId = :carId
                          and b3.createdAt = b.createdAt
                      )
                    order by b.createdAt desc, b.id desc
                    """,
            countQuery = """
                    select count(distinct b.branch.id) from Booking b
                    where b.customerUserId = :userId
                      and b.carId = :carId
                    """
    )
    Page<Long> findLatestIdPerBranchByCarId(
            @Param("userId") Long userId, @Param("carId") Long carId, Pageable pageable);

    @Query(
            value = """
                    select b.id from Booking b
                    where b.customerUserId = :userId
                      and b.status in :statuses
                      and b.createdAt = (
                        select max(b2.createdAt) from Booking b2
                        where b2.customerUserId = :userId
                          and b2.branch.id = b.branch.id
                          and b2.status in :statuses
                      )
                      and b.id = (
                        select max(b3.id) from Booking b3
                        where b3.customerUserId = :userId
                          and b3.branch.id = b.branch.id
                          and b3.status in :statuses
                          and b3.createdAt = b.createdAt
                      )
                    order by b.createdAt desc, b.id desc
                    """,
            countQuery = """
                    select count(distinct b.branch.id) from Booking b
                    where b.customerUserId = :userId
                      and b.status in :statuses
                    """
    )
    Page<Long> findLatestIdPerBranchByStatusIn(
            @Param("userId") Long userId, @Param("statuses") Collection<String> statuses, Pageable pageable);

    @Query(
            value = """
                    select b.id from Booking b
                    where b.customerUserId = :userId
                      and b.carId = :carId
                      and b.status in :statuses
                      and b.createdAt = (
                        select max(b2.createdAt) from Booking b2
                        where b2.customerUserId = :userId
                          and b2.branch.id = b.branch.id
                          and b2.carId = :carId
                          and b2.status in :statuses
                      )
                      and b.id = (
                        select max(b3.id) from Booking b3
                        where b3.customerUserId = :userId
                          and b3.branch.id = b.branch.id
                          and b3.carId = :carId
                          and b3.status in :statuses
                          and b3.createdAt = b.createdAt
                      )
                    order by b.createdAt desc, b.id desc
                    """,
            countQuery = """
                    select count(distinct b.branch.id) from Booking b
                    where b.customerUserId = :userId
                      and b.carId = :carId
                      and b.status in :statuses
                    """
    )
    Page<Long> findLatestIdPerBranchByCarIdAndStatusIn(
            @Param("userId") Long userId,
            @Param("carId") Long carId,
            @Param("statuses") Collection<String> statuses,
            Pageable pageable);

    /**
     * tr: purpose=all. Bütün kayıtlar, created_at azalan, eşitlikte id azalan.
     * en: purpose=all. Every row, created_at descending, then id descending.
     */
    @Query(
            value = """
                    select b.id from Booking b
                    where b.customerUserId = :userId
                    order by b.createdAt desc, b.id desc
                    """,
            countQuery = """
                    select count(b) from Booking b
                    where b.customerUserId = :userId
                    """
    )
    Page<Long> findAllIds(@Param("userId") Long userId, Pageable pageable);

    @Query(
            value = """
                    select b.id from Booking b
                    where b.customerUserId = :userId
                      and b.carId = :carId
                    order by b.createdAt desc, b.id desc
                    """,
            countQuery = """
                    select count(b) from Booking b
                    where b.customerUserId = :userId
                      and b.carId = :carId
                    """
    )
    Page<Long> findAllIdsByCarId(
            @Param("userId") Long userId, @Param("carId") Long carId, Pageable pageable);

    @Query(
            value = """
                    select b.id from Booking b
                    where b.customerUserId = :userId
                      and b.status in :statuses
                    order by b.createdAt desc, b.id desc
                    """,
            countQuery = """
                    select count(b) from Booking b
                    where b.customerUserId = :userId
                      and b.status in :statuses
                    """
    )
    Page<Long> findAllIdsByStatusIn(
            @Param("userId") Long userId, @Param("statuses") Collection<String> statuses, Pageable pageable);

    @Query(
            value = """
                    select b.id from Booking b
                    where b.customerUserId = :userId
                      and b.carId = :carId
                      and b.status in :statuses
                    order by b.createdAt desc, b.id desc
                    """,
            countQuery = """
                    select count(b) from Booking b
                    where b.customerUserId = :userId
                      and b.carId = :carId
                      and b.status in :statuses
                    """
    )
    Page<Long> findAllIdsByCarIdAndStatusIn(
            @Param("userId") Long userId,
            @Param("carId") Long carId,
            @Param("statuses") Collection<String> statuses,
            Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "branch.partner", "range", "range.calendar"})
    @Query("select b from Booking b where b.id in :ids")
    List<Booking> findForMineByIdIn(@Param("ids") Collection<Long> ids);

    @Query("select b.status, count(b) from Booking b where b.customerUserId = :userId group by b.status")
    List<Object[]> countGroupByStatus(@Param("userId") Long userId);

    @Query("select b.status, count(b) from Booking b where b.customerUserId = :userId and b.carId = :carId group by b.status")
    List<Object[]> countGroupByStatusAndCarId(@Param("userId") Long userId, @Param("carId") Long carId);
}

