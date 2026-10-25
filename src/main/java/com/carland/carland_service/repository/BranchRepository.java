package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByPartnerOrderByIdAsc(Partner partner);

    List<Branch> findByPartnerIdOrderByIdAsc(Long partnerId);

    Optional<Branch> findByIdAndPartner_Id(Long id, Long partnerId);

    @Query("select b from Branch b join fetch b.partner p where b.active = true and p.active = true order by p.id asc, b.id asc")
    List<Branch> findActiveWithActivePartner();
}
