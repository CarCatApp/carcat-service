package com.carland.carland_service.idda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * tr: IDDA plaka sorgu kayıtları. Hak sayımı countsTowardLimit ve queriedAt ile yapılır.
 * en: IDDA plate check rows. The cap counts rows with countsTowardLimit and queriedAt.
 */
public interface IddaPlateCheckRepository extends JpaRepository<IddaPlateCheck, Long>, JpaSpecificationExecutor<IddaPlateCheck> {

    List<IddaPlateCheck> findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(
            String plateKey,
            LocalDateTime queriedAt
    );

    /**
     * tr: VIN başına sorgu sayısı. Anahtar büyük harf. Boş listeyle çağırma.
     * en: Check count per VIN. The key is uppercase. Do not call with an empty list.
     */
    @Query("""
            select upper(c.vin), count(c)
            from IddaPlateCheck c
            where c.vin is not null and upper(c.vin) in :vins
            group by upper(c.vin)
            """)
    List<Object[]> countGroupedByVin(@Param("vins") Collection<String> vins);

    /**
     * tr: Bir VIN'in bütün sorguları, yeniden eskiye. VIN büyük harf.
     * en: Every check for one VIN, newest first. VIN is uppercase.
     */
    @Query("""
            select c from IddaPlateCheck c
            where c.vin is not null and upper(c.vin) = :vin
            order by c.queriedAt desc, c.id desc
            """)
    List<IddaPlateCheck> findByVinNewestFirst(@Param("vin") String vin);

    /**
     * tr: Birden fazla VIN. Sıra yeniden eskiye; gruplarken bu sıra korunur. Boş listeyle çağırma.
     * en: Several VINs. Newest first; grouping keeps that order. Do not call with an empty list.
     */
    @Query("""
            select c from IddaPlateCheck c
            where c.vin is not null and upper(c.vin) in :vins
            order by c.queriedAt desc, c.id desc
            """)
    List<IddaPlateCheck> findByVinInNewestFirst(@Param("vins") Collection<String> vins);
}
