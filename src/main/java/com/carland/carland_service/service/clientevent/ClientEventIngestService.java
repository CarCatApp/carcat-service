package com.carland.carland_service.service.clientevent;

import com.carland.carland_service.config.ClientEventProperties;
import com.carland.carland_service.entity.ClientErrorEvent;
import com.carland.carland_service.entity.ClientErrorIssue;
import com.carland.carland_service.repository.ClientErrorEventRepository;
import com.carland.carland_service.repository.ClientErrorExpectedRuleRepository;
import com.carland.carland_service.repository.ClientErrorIssueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * tr: Batch-i sinxron Postgres-ə yazır. Növbə yoxdur. Bir hadisə pisdirsə qalanı yazılır, cavab 204-dür.
 *     eventId təkrarı ikinci sətir açmır.
 * en: Writes the batch to Postgres in the request. No queue. One bad event does not drop the rest; the reply is 204.
 *     A repeated eventId does not insert a second row.
 */
@Slf4j
@Service
public class ClientEventIngestService {

    private final ObjectMapper mapper;
    private final ClientEventProperties properties;
    private final ClientEventQuota quota;
    private final ClientErrorEventRepository events;
    private final ClientErrorIssueRepository issues;
    private final ClientErrorExpectedRuleRepository rules;
    private final EntityManager entityManager;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ClientEventIngestService(ObjectMapper mapper,
                                    ClientEventProperties properties,
                                    ClientEventQuota quota,
                                    ClientErrorEventRepository events,
                                    ClientErrorIssueRepository issues,
                                    ClientErrorExpectedRuleRepository rules,
                                    EntityManager entityManager,
                                    PlatformTransactionManager transactionManager,
                                    Clock clock) {
        this.mapper = mapper;
        this.properties = properties;
        this.quota = quota;
        this.events = events;
        this.issues = issues;
        this.rules = rules;
        this.entityManager = entityManager;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public IngestOutcome ingest(byte[] body, String clientIp) {
        if (body == null || body.length > properties.getMaxBodyBytes()) {
            return IngestOutcome.tooLarge();
        }
        if (!quota.allowIp(clientIp)) {
            return IngestOutcome.limited();
        }
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception ex) {
            return IngestOutcome.bad();
        }
        if (root == null || !root.isObject()) {
            return IngestOutcome.bad();
        }
        JsonNode batch = root.get("events");
        if (batch == null || !batch.isArray()) {
            return IngestOutcome.bad();
        }
        int size = batch.size();
        if (size < 1 || size > properties.getMaxBatch()) {
            return IngestOutcome.bad();
        }
        Map<String, Integer> byInstall = new HashMap<>();
        for (JsonNode node : batch) {
            if (node != null && node.isObject()) {
                JsonNode install = node.get("installId");
                if (install != null && install.isTextual() && !install.asText().isBlank()) {
                    byInstall.merge(install.asText().trim(), 1, Integer::sum);
                }
            }
        }
        for (Map.Entry<String, Integer> entry : byInstall.entrySet()) {
            if (!quota.allowInstall(entry.getKey(), entry.getValue())) {
                return IngestOutcome.limited();
            }
        }

        int accepted = 0;
        for (JsonNode node : batch) {
            ClientEventParser.Parsed parsed = ClientEventParser.parse(node, properties, clock);
            if (parsed == null) {
                continue;
            }
            if (store(parsed)) {
                accepted++;
            }
        }
        log.info("client-events accepted={} dropped={}", accepted, size - accepted);
        return IngestOutcome.ok(accepted);
    }

    private boolean store(ClientEventParser.Parsed parsed) {
        String fingerprint = fingerprint(parsed);
        ClientErrorIssue issue;
        try {
            issue = findOrCreate(parsed, fingerprint);
        } catch (RuntimeException ex) {
            log.warn("client-event issue failed eventId={} type={}", parsed.eventId(), ex.getClass().getSimpleName());
            return false;
        }
        if (issue == null || issue.getId() == null) {
            return false;
        }
        long issueId = issue.getId();
        try {
            Boolean ok = tx.execute(status -> write(issueId, parsed, fingerprint));
            return Boolean.TRUE.equals(ok);
        } catch (DataIntegrityViolationException ex) {
            if (events.existsByEventId(parsed.eventId())) {
                return true;
            }
            log.warn("client-event store failed eventId={}", parsed.eventId());
            return false;
        } catch (RuntimeException ex) {
            log.warn("client-event store failed eventId={} type={}", parsed.eventId(), ex.getClass().getSimpleName());
            return false;
        }
    }

    private ClientErrorIssue findOrCreate(ClientEventParser.Parsed parsed, String fingerprint) {
        return issues.findByFingerprintAndFingerprintVersion(fingerprint, properties.getFingerprintVersion())
                .orElseGet(() -> {
                    try {
                        return tx.execute(status -> issues.saveAndFlush(newIssue(parsed, fingerprint)));
                    } catch (DataIntegrityViolationException ex) {
                        return issues.findByFingerprintAndFingerprintVersion(
                                        fingerprint, properties.getFingerprintVersion())
                                .orElseThrow(() -> ex);
                    }
                });
    }

    private boolean write(long issueId, ClientEventParser.Parsed parsed, String fingerprint) {
        if (events.existsByEventId(parsed.eventId())) {
            return true;
        }
        ClientErrorIssue issue = issues.findById(issueId).orElseThrow();
        ClientErrorEvent row = newEvent(parsed, issue, fingerprint);
        events.saveAndFlush(row);

        issue.setOccurrenceCount(issue.getOccurrenceCount() + 1);
        if (parsed.occurredAt().isBefore(issue.getFirstSeen())) {
            issue.setFirstSeen(parsed.occurredAt());
        }
        if (parsed.occurredAt().isAfter(issue.getLastSeen())) {
            issue.setLastSeen(parsed.occurredAt());
        }
        if (parsed.userId() != null && linkUser(issue.getId(), parsed.userId()) > 0) {
            issue.setAffectedUserCount(issue.getAffectedUserCount() + 1);
        }
        if (linkInstall(issue.getId(), parsed.installId()) > 0) {
            issue.setAffectedInstallCount(issue.getAffectedInstallCount() + 1);
        }
        if (quota.storm(fingerprint)) {
            issue.setHighFrequency(true);
        }
        issues.save(issue);
        return true;
    }

    private String fingerprint(ClientEventParser.Parsed parsed) {
        int version = properties.getFingerprintVersion();
        if ("apiError".equals(parsed.eventType())) {
            return ClientEventFingerprint.api(version, parsed.statusCode(), parsed.signal(),
                    parsed.endpoint() == null ? "" : parsed.endpoint());
        }
        return ClientEventFingerprint.fatal(version, parsed.eventType(), parsed.message(), parsed.stackTrace());
    }

    private boolean expected(ClientEventParser.Parsed parsed) {
        if (!"apiError".equals(parsed.eventType()) || parsed.endpoint() == null || parsed.endpoint().isBlank()) {
            return false;
        }
        if (parsed.statusCode() != null) {
            return rules.existsByEndpointPatternAndStatusCodeAndExpectedTrue(parsed.endpoint(), parsed.statusCode());
        }
        return !rules.findOpen(parsed.endpoint(), parsed.signal()).isEmpty();
    }

    private ClientErrorIssue newIssue(ClientEventParser.Parsed parsed, String fingerprint) {
        ClientErrorIssue issue = new ClientErrorIssue();
        issue.setFingerprint(fingerprint);
        issue.setFingerprintVersion(properties.getFingerprintVersion());
        issue.setEventType(parsed.eventType());
        issue.setTitle(title(parsed));
        issue.setStatusCode(parsed.statusCode());
        issue.setEndpoint(parsed.endpoint());
        issue.setFirstSeen(parsed.occurredAt());
        issue.setLastSeen(parsed.occurredAt());
        issue.setOccurrenceCount(0);
        issue.setAffectedUserCount(0);
        issue.setAffectedInstallCount(0);
        issue.setHighFrequency(false);
        issue.setIssueStatus(ClientErrorIssue.STATUS_NEW);
        return issue;
    }

    private ClientErrorEvent newEvent(ClientEventParser.Parsed parsed, ClientErrorIssue issue, String fingerprint) {
        ClientErrorEvent row = new ClientErrorEvent();
        row.setEventId(parsed.eventId());
        row.setEventType(parsed.eventType());
        row.setOccurredAt(parsed.occurredAt());
        row.setReceivedAt(LocalDateTime.now(ZoneOffset.UTC));
        row.setMessage(parsed.message());
        row.setStackTrace(parsed.stackTrace());
        row.setSessionId(parsed.sessionId());
        row.setInstallId(parsed.installId());
        row.setUserId(parsed.userId());
        row.setAppVersion(parsed.appVersion());
        row.setBuildNumber(parsed.buildNumber());
        row.setPackageName(parsed.packageName());
        row.setOsName(parsed.osName());
        row.setOsVersion(parsed.osVersion());
        row.setMemoryRssBytes(parsed.memoryRssBytes());
        row.setNetwork(parsed.network());
        row.setEnvironment(parsed.environment());
        row.setContextLibrary(parsed.contextLibrary());
        row.setContextRoute(parsed.contextRoute());
        row.setStatusCode(parsed.statusCode());
        row.setEndpoint(parsed.endpoint());
        row.setExpected(expected(parsed));
        row.setFingerprint(fingerprint);
        row.setFingerprintVersion(properties.getFingerprintVersion());
        row.setIssueId(issue.getId());
        row.setBreadcrumbs(parsed.breadcrumbs());
        return row;
    }

    private static String title(ClientEventParser.Parsed parsed) {
        String raw;
        if ("apiError".equals(parsed.eventType())) {
            String status = parsed.statusCode() != null
                    ? parsed.statusCode() + " "
                    : (parsed.signal() == null ? "" : parsed.signal() + " ");
            raw = (status + (parsed.endpoint() == null ? parsed.message() : parsed.endpoint())).trim();
        } else {
            raw = parsed.message();
        }
        if (raw.length() <= 180) {
            return raw;
        }
        return raw.substring(0, 180);
    }

    private int linkUser(long issueId, long userId) {
        return entityManager.createNativeQuery("""
                        INSERT INTO client_error_issue_user (issue_id, user_id)
                        VALUES (:issueId, :userId)
                        ON CONFLICT DO NOTHING
                        """)
                .setParameter("issueId", issueId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    private int linkInstall(long issueId, String installId) {
        return entityManager.createNativeQuery("""
                        INSERT INTO client_error_issue_install (issue_id, install_id)
                        VALUES (:issueId, :installId)
                        ON CONFLICT DO NOTHING
                        """)
                .setParameter("issueId", issueId)
                .setParameter("installId", installId)
                .executeUpdate();
    }

    public record IngestOutcome(int httpStatus, int accepted) {
        public static IngestOutcome ok(int accepted) {
            return new IngestOutcome(204, accepted);
        }

        public static IngestOutcome bad() {
            return new IngestOutcome(400, 0);
        }

        public static IngestOutcome tooLarge() {
            return new IngestOutcome(413, 0);
        }

        public static IngestOutcome limited() {
            return new IngestOutcome(429, 0);
        }
    }
}
