package com.carland.carland_service.service;

import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagAudit;
import com.carland.carland_service.entity.FeatureFlagEndpoint;
import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureFlagAdminSupportTest {

    @Test
    void liveFlagName_attached() {
        FeatureFlag flag = FeatureFlag.builder().name("TEST_FLOW").build();
        assertEquals("TEST_FLOW", FeatureFlagAdminSupport.liveFlagName(flag));
    }

    @Test
    void liveFlagName_unattached() {
        assertNull(FeatureFlagAdminSupport.liveFlagName(null));
    }

    @Test
    void liveFlagName_softDeleted() {
        FeatureFlag flag = FeatureFlag.builder()
                .name("GONE")
                .deletedAt(LocalDateTime.now())
                .build();
        assertNull(FeatureFlagAdminSupport.liveFlagName(flag));
    }

    @Test
    void endpointDto_usesLiveFlagName() {
        FeatureFlag live = FeatureFlag.builder().id(1L).name("TEST_FLOW").build();
        FeatureFlag dead = FeatureFlag.builder().id(2L).name("OLD").deletedAt(LocalDateTime.now()).build();

        Map<String, Object> attached = FeatureFlagAdminSupport.endpointDto(
                FeatureFlagEndpoint.builder().id(10L).httpMethod("POST").pathPattern("/api/v1/x").flag(live).build());
        assertEquals("TEST_FLOW", attached.get("flagName"));
        assertEquals(true, attached.get("claimed"));

        Map<String, Object> free = FeatureFlagAdminSupport.endpointDto(
                FeatureFlagEndpoint.builder().id(11L).httpMethod("GET").pathPattern("/api/v1/y").build());
        assertNull(free.get("flagName"));
        assertEquals(false, free.get("claimed"));

        Map<String, Object> orphan = FeatureFlagAdminSupport.endpointDto(
                FeatureFlagEndpoint.builder().id(12L).httpMethod("GET").pathPattern("/api/v1/z").flag(dead).build());
        assertNull(orphan.get("flagName"));
        assertEquals(false, orphan.get("claimed"));
    }

    @Test
    void pageRequest_allows10And25() {
        assertEquals(10, FeatureFlagAdminSupport.pageRequest(0, 10).getPageSize());
        assertEquals(25, FeatureFlagAdminSupport.pageRequest(1, 25).getPageSize());
        assertEquals(1, FeatureFlagAdminSupport.pageRequest(1, 25).getPageNumber());
    }

    @Test
    void pageRequest_rejectsInvalidSizeAndNegativePage() {
        assertThrows(IllegalArgumentException.class, () -> FeatureFlagAdminSupport.pageRequest(0, 15));
        assertThrows(IllegalArgumentException.class, () -> FeatureFlagAdminSupport.pageRequest(0, 1));
        assertThrows(IllegalArgumentException.class, () -> FeatureFlagAdminSupport.pageRequest(-1, 10));
    }

    @Test
    void envelope_beyondLastPage() {
        Page<String> empty = new PageImpl<>(List.of(), PageRequest.of(9, 10), 11);
        Map<String, Object> body = FeatureFlagAdminSupport.envelope(empty, List.of());
        assertEquals(9, body.get("page"));
        assertEquals(10, body.get("size"));
        assertEquals(11L, body.get("totalElements"));
        assertEquals(2, body.get("totalPages"));
        assertTrue(((List<?>) body.get("content")).isEmpty());
    }

    @Test
    void auditChange_prefersStoredFlagName() {
        FeatureFlagAudit row = FeatureFlagAudit.builder()
                .httpMethod("STATE")
                .flagName("TEST_FLOW")
                .pathPattern("TEST_FLOW USER")
                .role(UserRoles.USER)
                .oldState(FeatureFlagState.ENABLED)
                .newState(FeatureFlagState.HIDDEN)
                .build();
        Map<String, Object> dto = FeatureFlagAdminSupport.auditDto(row);
        assertEquals("TEST_FLOW", dto.get("flagName"));
        assertEquals("TEST_FLOW USER → HIDDEN", dto.get("change"));
    }

    @Test
    void auditChange_legacyStateRowWithoutStoredName() {
        FeatureFlagAudit row = FeatureFlagAudit.builder()
                .httpMethod("STATE")
                .pathPattern("TEST_FLOW USER")
                .role(UserRoles.ADMIN)
                .newState(FeatureFlagState.HIDDEN)
                .build();
        assertEquals("TEST_FLOW", FeatureFlagAdminSupport.resolveStoredFlagName(row));
        assertEquals("TEST_FLOW USER → HIDDEN", FeatureFlagAdminSupport.auditDto(row).get("change"));
    }
}
