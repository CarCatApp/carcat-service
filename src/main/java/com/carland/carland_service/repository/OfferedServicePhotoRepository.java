package com.carland.carland_service.repository;

import com.carland.carland_service.entity.OfferedServicePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OfferedServicePhotoRepository extends JpaRepository<OfferedServicePhoto, Long> {

    OfferedServicePhoto findByOfferedServiceId(Long offeredServiceId);

    @Query("""
            select p.offeredServiceId from OfferedServicePhoto p
            where p.offeredServiceId in :ids and p.imageData is not null
            """)
    List<Long> findOfferedServiceIdsWithImage(@Param("ids") Collection<Long> ids);
}
