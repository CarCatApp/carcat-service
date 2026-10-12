package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BranchIndividualService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * tr: Şube fərdi xidmət fiyatı ve durumu.
 * en: Branch individual-service price and status.
 */
@Repository
public interface BranchIndividualServiceRepository extends JpaRepository<BranchIndividualService, Long> {

    List<BranchIndividualService> findByBranch_Id(Long branchId);

    Optional<BranchIndividualService> findByBranch_IdAndIndividualService_Id(Long branchId, Long individualServiceId);

    void deleteByIndividualService_Id(Long individualServiceId);

    long countByIndividualService_Id(Long individualServiceId);

    /** Active catalog row, branch row on, and at least one price tier set. */
    @Query("select b.id from BranchIndividualService row join row.branch b join row.individualService s "
            + "where s.id = :serviceId and row.active = true and s.active = true "
            + "and (row.priceSimple is not null or row.priceMedium is not null or row.priceComplex is not null)")
    List<Long> findActivePricedBranchIds(@Param("serviceId") Long serviceId);
}
