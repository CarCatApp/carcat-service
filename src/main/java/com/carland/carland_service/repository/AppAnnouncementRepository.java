package com.carland.carland_service.repository;

import com.carland.carland_service.entity.AppAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppAnnouncementRepository extends JpaRepository<AppAnnouncement, Long> {

    @Query("""
            SELECT DISTINCT a FROM AppAnnouncement a
            LEFT JOIN FETCH a.translations
            WHERE a.active = true
            """)
    List<AppAnnouncement> findActiveWithTranslations();

    @Query("""
            SELECT DISTINCT a FROM AppAnnouncement a
            LEFT JOIN FETCH a.translations
            WHERE a.id = :id
            """)
    Optional<AppAnnouncement> findWithTranslationsById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT a FROM AppAnnouncement a
            LEFT JOIN FETCH a.translations
            """)
    List<AppAnnouncement> findAllWithTranslations();
}
