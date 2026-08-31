package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * tr: Feedback entity'si için JPA repository; geri bildirim kayıtlarına standart CRUD erişimi sağlar.
 * en: JPA repository for the Feedback entity; provides standard CRUD access to feedback records.
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("""
            SELECT f FROM Feedback f
            WHERE (:type IS NULL OR LOWER(f.type) = LOWER(:type))
              AND (:phone IS NULL OR (f.customerPhone IS NOT NULL
                   AND LOWER(f.customerPhone) LIKE LOWER(CONCAT('%', :phone, '%'))))
              AND (:withPhoto = false OR EXISTS (
                   SELECT 1 FROM FeedbackPhoto p WHERE p.feedbackId = f.feedbackId))
            ORDER BY f.feedbackId DESC
            """)
    Page<Feedback> search(
            @Param("type") String type,
            @Param("phone") String phone,
            @Param("withPhoto") boolean withPhoto,
            Pageable pageable);
}
