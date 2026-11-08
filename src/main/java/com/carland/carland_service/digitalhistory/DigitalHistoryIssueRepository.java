package com.carland.carland_service.digitalhistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DigitalHistoryIssueRepository extends JpaRepository<DigitalHistoryIssue, Long> {

    Optional<DigitalHistoryIssue> findByCode(String code);

    boolean existsByCode(String code);
}
