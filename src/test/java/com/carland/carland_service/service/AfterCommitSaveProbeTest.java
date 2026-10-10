package com.carland.carland_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * tr: afterCommit içinde REQUIRED save'in yeni commit açmadığını ölçer.
 * en: Measures that a REQUIRED save inside afterCommit does not open its own commit.
 */
class AfterCommitSaveProbeTest {

    @Test
    void requiredSaveInsideAfterCommitJoinsAndDoesNotCommit() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.getAutoCommit()).thenReturn(true);
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);

        AtomicBoolean txActive = new AtomicBoolean(false);
        AtomicBoolean syncActive = new AtomicBoolean(false);
        AtomicBoolean requiredIsNew = new AtomicBoolean(true);
        AtomicBoolean requiresNewIsNew = new AtomicBoolean(false);

        new TransactionTemplate(manager).executeWithoutResult(status ->
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        txActive.set(TransactionSynchronizationManager.isActualTransactionActive());
                        syncActive.set(TransactionSynchronizationManager.isSynchronizationActive());
                        new TransactionTemplate(manager).executeWithoutResult(inner ->
                                requiredIsNew.set(inner.isNewTransaction()));
                        TransactionTemplate fresh = new TransactionTemplate(manager);
                        fresh.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        fresh.executeWithoutResult(inner -> requiresNewIsNew.set(inner.isNewTransaction()));
                    }
                }));

        String data = "{\"txActive\":" + txActive.get()
                + ",\"syncActive\":" + syncActive.get()
                + ",\"requiredIsNew\":" + requiredIsNew.get()
                + ",\"requiresNewIsNew\":" + requiresNewIsNew.get() + "}";
        String line = "{\"sessionId\":\"f25d4f\",\"runId\":\"probe\",\"hypothesisId\":\"B\","
                + "\"location\":\"AfterCommitSaveProbeTest\",\"message\":\"afterCommit transaction join\","
                + "\"data\":" + data + ",\"timestamp\":" + System.currentTimeMillis() + "}\n";
        Files.write(Path.of("c:/Users/Aziz/IdeaProjects/debug-f25d4f.log"),
                line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        assertTrue(txActive.get());
        assertTrue(syncActive.get());
        assertFalse(requiredIsNew.get());
        assertTrue(requiresNewIsNew.get());
        verify(connection, times(2)).commit();
    }
}
