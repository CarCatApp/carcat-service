package com.carland.carland_service.idda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * tr: IDDA plaka sorgu kayıtları. Hak sayımı countsTowardLimit ve queriedAt ile yapılır.
 * en: IDDA plate check rows. The cap counts rows with countsTowardLimit and queriedAt.
 */
public interface IddaPlateCheckRepository extends JpaRepository<IddaPlateCheck, Long> {

    List<IddaPlateCheck> findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(
            String plateKey,
            LocalDateTime queriedAt
    );
}
