package com.carland.carland_service.repository;

import com.carland.carland_service.entity.ClientErrorExpectedRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientErrorExpectedRuleRepository extends JpaRepository<ClientErrorExpectedRule, Long> {

    boolean existsByEndpointPatternAndStatusCodeAndExpectedTrue(String endpointPattern, Integer statusCode);

    @Query("""
            select r from ClientErrorExpectedRule r
            where r.endpointPattern = :endpoint
              and r.expected = true
              and r.statusCode is null
              and (r.signal is null or r.signal = :signal)
            """)
    List<ClientErrorExpectedRule> findOpen(@Param("endpoint") String endpoint, @Param("signal") String signal);
}
