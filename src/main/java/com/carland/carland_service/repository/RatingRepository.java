package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    @Query("SELECT r FROM Rating r JOIN FETCH r.branch WHERE r.branch.id IN :branchIds")
    List<Rating> findByBranch_IdIn(@Param("branchIds") Collection<Long> branchIds);
}
