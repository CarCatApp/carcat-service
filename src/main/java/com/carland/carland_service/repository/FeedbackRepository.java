package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: Feedback entity'si için JPA repository; geri bildirim kayıtlarına standart CRUD erişimi sağlar.
 * en: JPA repository for the Feedback entity; provides standard CRUD access to feedback records.
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
