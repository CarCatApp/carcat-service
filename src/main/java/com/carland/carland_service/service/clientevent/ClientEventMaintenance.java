package com.carland.carland_service.service.clientevent;

import com.carland.carland_service.config.ClientEventProperties;
import com.carland.carland_service.entity.ClientErrorIssue;
import com.carland.carland_service.entity.ClientErrorIssueUser;
import com.carland.carland_service.repository.ClientErrorEventRepository;
import com.carland.carland_service.repository.ClientErrorIssueRepository;
import com.carland.carland_service.repository.ClientErrorIssueUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * tr: Ham hadisə 30 gündən sonra silinir. Hesab silinəndə userId boşalır, hadisə və sayğac qalır.
 *     Təsir sayı bir azalır, çünki həmin istifadəçi artıq sətirə bağlana bilməz.
 * en: Raw events are deleted after 30 days. Account deletion clears userId and keeps the event.
 *     The affected-user count drops by one because that user can no longer be tied to the row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientEventMaintenance {

    private final ClientEventProperties properties;
    private final ClientErrorEventRepository events;
    private final ClientErrorIssueRepository issues;
    private final ClientErrorIssueUserRepository issueUsers;

    @Transactional
    public int purgeReceivedBeforeNow() {
        int days = Math.max(properties.getRetentionDays(), 1);
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(days);
        return events.deleteByReceivedAtBefore(cutoff);
    }

    @Transactional
    public void anonymize(long userId) {
        for (ClientErrorIssueUser link : issueUsers.findByUserId(userId)) {
            issues.findById(link.getIssueId()).ifPresent(issue -> decrementUser(issue));
        }
        issueUsers.deleteByUserId(userId);
        int cleared = events.clearUserId(userId);
        log.info("client-events anonymize userId={} cleared={}", userId, cleared);
    }

    private void decrementUser(ClientErrorIssue issue) {
        if (issue.getAffectedUserCount() > 0) {
            issue.setAffectedUserCount(issue.getAffectedUserCount() - 1);
            issues.save(issue);
        }
    }
}
