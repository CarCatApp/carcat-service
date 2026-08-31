package com.carland.carland_service.repository;

import com.carland.carland_service.entity.FeedbackPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackPhotoRepository extends JpaRepository<FeedbackPhoto, Long> {

    Optional<FeedbackPhoto> findByFeedbackId(Long feedbackId);

    boolean existsByFeedbackId(Long feedbackId);

    @Query("select p.feedbackId from FeedbackPhoto p where p.feedbackId in :ids")
    List<Long> findFeedbackIdsWithPhoto(@Param("ids") Collection<Long> ids);
}
