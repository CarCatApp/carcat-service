package com.carland.carland_service.service.clientevent;

import com.carland.carland_service.config.ClientEventProperties;
import com.carland.carland_service.entity.ClientErrorEvent;
import com.carland.carland_service.entity.ClientErrorIssue;
import com.carland.carland_service.repository.ClientErrorEventRepository;
import com.carland.carland_service.repository.ClientErrorExpectedRuleRepository;
import com.carland.carland_service.repository.ClientErrorIssueRepository;
import com.carland.carland_service.service.clientevent.ClientEventIngestService.IngestOutcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientEventIngestServiceTest {

    @Mock
    private ClientEventQuota quota;
    @Mock
    private ClientErrorEventRepository events;
    @Mock
    private ClientErrorIssueRepository issues;
    @Mock
    private ClientErrorExpectedRuleRepository rules;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private ClientEventIngestService service;
    private final AtomicReference<ClientErrorIssue> stored = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        lenient().when(quota.allowIp(anyString())).thenReturn(true);
        lenient().when(quota.allowInstall(anyString(), anyInt())).thenReturn(true);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), any())).thenReturn(query);
        lenient().when(query.executeUpdate()).thenReturn(1);
        lenient().when(issues.findByFingerprintAndFingerprintVersion(anyString(), anyInt()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        lenient().when(issues.saveAndFlush(any())).thenAnswer(invocation -> {
            ClientErrorIssue issue = invocation.getArgument(0);
            issue.setId(10L);
            stored.set(issue);
            return issue;
        });
        lenient().when(issues.findById(10L)).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        lenient().when(events.existsByEventId(anyString())).thenReturn(false);
        lenient().when(events.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ClientEventIngestService(
                new ObjectMapper(),
                new ClientEventProperties(),
                quota,
                events,
                issues,
                rules,
                entityManager,
                immediateTransactions(),
                Clock.fixed(Instant.parse("2026-11-02T06:20:00Z"), ZoneOffset.UTC));
    }

    @Test
    void rejectsABrokenBatch() {
        assertEquals(400, service.ingest("[]".getBytes(StandardCharsets.UTF_8), "10.0.0.1").httpStatus());
        assertEquals(400, service.ingest("{\"events\":[]}".getBytes(StandardCharsets.UTF_8), "10.0.0.1").httpStatus());
        StringBuilder tooMany = new StringBuilder("{\"events\":[");
        for (int i = 0; i < 51; i++) {
            if (i > 0) {
                tooMany.append(',');
            }
            tooMany.append("{}");
        }
        tooMany.append("]}");
        assertEquals(400, service.ingest(tooMany.toString().getBytes(StandardCharsets.UTF_8), "10.0.0.1").httpStatus());
    }

    @Test
    void keepsValidEventsWhenOneIsBroken() {
        String body = "{\"events\":[" + fatal("11111111-1111-4111-8111-111111111111") + ",{\"type\":\"fatal\"}]}";
        IngestOutcome outcome = service.ingest(body.getBytes(StandardCharsets.UTF_8), "10.0.0.1");
        assertEquals(204, outcome.httpStatus());
        assertEquals(1, outcome.accepted());
        verify(events).saveAndFlush(any(ClientErrorEvent.class));
    }

    @Test
    void duplicateEventIdDoesNotInsertAgain() {
        ClientErrorIssue issue = new ClientErrorIssue();
        issue.setId(10L);
        issue.setOccurrenceCount(4);
        issue.setFirstSeen(java.time.LocalDateTime.parse("2026-11-02T06:00:00"));
        issue.setLastSeen(issue.getFirstSeen());
        issue.setAffectedUserCount(1);
        issue.setAffectedInstallCount(1);
        stored.set(issue);
        when(events.existsByEventId(anyString())).thenReturn(true);
        IngestOutcome outcome = service.ingest(
                ("{\"events\":[" + fatal("11111111-1111-4111-8111-111111111111") + "]}").getBytes(StandardCharsets.UTF_8),
                "10.0.0.1");
        assertEquals(1, outcome.accepted());
        assertEquals(4, issue.getOccurrenceCount());
        verify(events, never()).saveAndFlush(any());
    }

    @Test
    void apiRuleMarksTheEventExpected() {
        when(rules.existsByEndpointPatternAndStatusCodeAndExpectedTrue("GET /cars/vin/*", 401)).thenReturn(true);
        IngestOutcome outcome = service.ingest(
                ("{\"events\":[" + api("22222222-2222-4222-8222-222222222222") + "]}").getBytes(StandardCharsets.UTF_8),
                "10.0.0.1");
        assertEquals(204, outcome.httpStatus());
        ArgumentCaptor<ClientErrorEvent> saved = ArgumentCaptor.forClass(ClientErrorEvent.class);
        verify(events).saveAndFlush(saved.capture());
        assertTrue(saved.getValue().isExpected());
        assertEquals("GET /cars/vin/*", saved.getValue().getEndpoint());
    }

    @Test
    void installFloodIsRejected() {
        when(quota.allowInstall(anyString(), anyInt())).thenReturn(false);
        IngestOutcome outcome = service.ingest(
                ("{\"events\":[" + fatal("11111111-1111-4111-8111-111111111111") + "]}").getBytes(StandardCharsets.UTF_8),
                "10.0.0.1");
        assertEquals(429, outcome.httpStatus());
        verify(events, never()).saveAndFlush(any());
    }

    private static String fatal(String eventId) {
        return """
                {
                  "eventId": "%s",
                  "type": "fatal",
                  "timestamp": "2026-11-02T06:15:00.000Z",
                  "message": "FormatException: User 123",
                  "stackTrace": "#0 Foo.bar (package:carcat/src/a.dart:12:3)",
                  "sessionId": "22222222-2222-4222-8222-222222222222",
                  "installId": "33333333-3333-4333-8333-333333333333",
                  "userId": 15,
                  "app": {"version": "1.4.0", "buildNumber": "42", "packageName": "com.nematmirzayev.carcat"},
                  "os": {"name": "ios", "version": "18.0"},
                  "environment": "release"
                }
                """.formatted(eventId);
    }

    private static String api(String eventId) {
        return """
                {
                  "eventId": "%s",
                  "type": "apiError",
                  "timestamp": "2026-11-02T06:15:00.000Z",
                  "message": "badResponse 401 GET /cars/vin/*",
                  "sessionId": "22222222-2222-4222-8222-222222222222",
                  "installId": "33333333-3333-4333-8333-333333333333",
                  "app": {"version": "1.4.0", "buildNumber": "42", "packageName": "com.nematmirzayev.carcat"},
                  "os": {"name": "ios", "version": "18.0"},
                  "environment": "release",
                  "context": {"statusCode": 401, "endpoint": "GET /cars/vin/*"}
                }
                """.formatted(eventId);
    }

    private static PlatformTransactionManager immediateTransactions() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }
}
