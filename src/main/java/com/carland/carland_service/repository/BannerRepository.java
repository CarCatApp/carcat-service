package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByStatusOrderByIdDesc(String status);

    List<Banner> findByStatusAndShowingOrderByIdDesc(String status, Boolean showing);

    List<Banner> findByShowingTrueOrderByIdDesc();

    Optional<Banner> findByPromoId(Long promoId);

    @Query("""
            SELECT b FROM Banner b
            WHERE b.status = :status AND b.showing = true
            ORDER BY CASE WHEN b.sortOrder IS NULL THEN 1 ELSE 0 END ASC,
                     b.sortOrder ASC,
                     b.id DESC
            """)
    List<Banner> findVisible(@Param("status") String status);

    @Query("""
            SELECT b FROM Banner b
            WHERE b.status = :status AND b.showing = true
              AND b.expireDate IS NOT NULL AND b.expireDate < :today
            """)
    List<Banner> findExpiredShowing(@Param("status") String status, @Param("today") LocalDate today);

    @Query("""
            SELECT b FROM Banner b
            WHERE b.status = :status
            ORDER BY CASE WHEN b.sortOrder IS NULL THEN 1 ELSE 0 END ASC,
                     b.sortOrder ASC,
                     b.id DESC
            """)
    List<Banner> findActiveOrdered(@Param("status") String status);
}
