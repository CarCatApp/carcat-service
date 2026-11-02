package com.carland.carland_service.repository;

import com.carland.carland_service.entity.ClientErrorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClientErrorEventRepository extends JpaRepository<ClientErrorEvent, Long>,
        JpaSpecificationExecutor<ClientErrorEvent> {

    boolean existsByEventId(String eventId);

    @Modifying
    @Query("update ClientErrorEvent e set e.userId = null where e.userId = :userId")
    int clearUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from ClientErrorEvent e where e.receivedAt < :cutoff")
    int deleteByReceivedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query(value = """
            select to_char(occurred_at + interval '4 hours', 'YYYY-MM-DD') as day, count(*)
            from client_error_events
            where user_id = :userId
              and occurred_at >= :from
              and occurred_at < :to
            group by 1
            order by 1
            """, nativeQuery = true)
    List<Object[]> countByDay(@Param("userId") Long userId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    @Query("""
            select e.eventType, count(e) from ClientErrorEvent e
            where e.userId = :userId and e.occurredAt >= :from and e.occurredAt < :to
            group by e.eventType
            """)
    List<Object[]> countByType(@Param("userId") Long userId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    @Query("""
            select max(e.occurredAt) from ClientErrorEvent e
            where e.userId = :userId and e.occurredAt >= :from and e.occurredAt < :to
            """)
    LocalDateTime lastSeen(@Param("userId") Long userId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to);

    @Query(value = """
            select issue_id, count(*)
            from client_error_events
            where user_id = :userId
              and occurred_at >= :from
              and occurred_at < :to
            group by issue_id
            order by count(*) desc
            limit 5
            """, nativeQuery = true)
    List<Object[]> topIssues(@Param("userId") Long userId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    @Query("""
            select e.osName, count(e) from ClientErrorEvent e
            where e.userId = :userId and e.occurredAt >= :from and e.occurredAt < :to
            group by e.osName
            order by count(e) desc
            """)
    List<Object[]> countByOs(@Param("userId") Long userId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    @Query("""
            select e.appVersion, e.buildNumber, count(e) from ClientErrorEvent e
            where e.userId = :userId and e.occurredAt >= :from and e.occurredAt < :to
            group by e.appVersion, e.buildNumber
            order by count(e) desc
            """)
    List<Object[]> countByApp(@Param("userId") Long userId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    @Query("""
            select e.network, count(e) from ClientErrorEvent e
            where e.userId = :userId and e.occurredAt >= :from and e.occurredAt < :to
            group by e.network
            order by count(e) desc
            """)
    List<Object[]> countByNetwork(@Param("userId") Long userId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    @Query("""
            select count(distinct e.installId) from ClientErrorEvent e
            where e.userId = :userId and e.occurredAt >= :from and e.occurredAt < :to
            """)
    long countInstalls(@Param("userId") Long userId,
                       @Param("from") LocalDateTime from,
                       @Param("to") LocalDateTime to);

    @Query("""
            select distinct e.installId from ClientErrorEvent e
            where e.userId = :userId and e.occurredAt >= :from and e.occurredAt < :to
            """)
    List<String> installIds(@Param("userId") Long userId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);
}
