package com.carland.carland_service.repository;

import com.carland.carland_service.entity.IndividualServiceSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * tr: Katalog xidmət id-sindən fərdi xidmət id-si.
 * en: Individual-service id for a catalog service id.
 */
@Repository
public interface IndividualServiceSourceRepository extends JpaRepository<IndividualServiceSource, Long> {

    @Query("select s.individualServiceId from IndividualServiceSource s where s.serviceId = :serviceId")
    List<Long> findIndividualServiceIdsByServiceId(@Param("serviceId") Long serviceId);

    List<IndividualServiceSource> findByIndividualServiceIdIn(Collection<Long> individualServiceIds);
}
