package com.carland.carland_service.repository;

import com.carland.carland_service.entity.ClientErrorIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientErrorIssueRepository extends JpaRepository<ClientErrorIssue, Long>,
        JpaSpecificationExecutor<ClientErrorIssue> {

    Optional<ClientErrorIssue> findByFingerprintAndFingerprintVersion(String fingerprint, int fingerprintVersion);
}
