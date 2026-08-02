package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * tr: Partner entity'si için JPA repository; iş ortaklarını ad ve kaynağa göre sorgular.
 * en: JPA repository for the Partner entity; queries partners by name and source.
 */
@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    /** tr: Ad ve kaynağa göre (büyük/küçük harf duyarsız) partneri bulur. / en: Finds a partner by name and source (case-insensitive). */
    Optional<Partner> findByNameIgnoreCaseAndSourceIgnoreCase(String name, String source);

    /** tr: Kaynağa göre aktif partneri bulur. / en: Finds the active partner by source. */
    Optional<Partner> findBySourceIgnoreCaseAndActiveTrue(String source);
}
