package com.carland.carland_service.repository;

import com.carland.carland_service.entity.ClientErrorIssueUser;
import com.carland.carland_service.entity.ClientErrorIssueUser.Pk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientErrorIssueUserRepository extends JpaRepository<ClientErrorIssueUser, Pk> {

    List<ClientErrorIssueUser> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
