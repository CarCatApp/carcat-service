package com.carland.carland_service.service.clientevent;

import com.carland.carland_service.config.ClientEventProperties;
import com.carland.carland_service.entity.ClientErrorIssue;
import com.carland.carland_service.entity.ClientErrorIssueUser;
import com.carland.carland_service.repository.ClientErrorEventRepository;
import com.carland.carland_service.repository.ClientErrorIssueRepository;
import com.carland.carland_service.repository.ClientErrorIssueUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientEventMaintenanceTest {

    @Mock
    private ClientErrorEventRepository events;
    @Mock
    private ClientErrorIssueRepository issues;
    @Mock
    private ClientErrorIssueUserRepository issueUsers;

    @Test
    void anonymizeClearsTheUserAndKeepsTheEvent() {
        ClientErrorIssue issue = new ClientErrorIssue();
        issue.setId(9L);
        issue.setAffectedUserCount(3);
        when(issueUsers.findByUserId(5L)).thenReturn(List.of(new ClientErrorIssueUser(9L, 5L)));
        when(issues.findById(9L)).thenReturn(Optional.of(issue));
        when(events.clearUserId(5L)).thenReturn(2);

        new ClientEventMaintenance(new ClientEventProperties(), events, issues, issueUsers).anonymize(5L);

        assertEquals(2, issue.getAffectedUserCount());
        verify(issueUsers).deleteByUserId(5L);
        verify(events).clearUserId(5L);
    }
}
