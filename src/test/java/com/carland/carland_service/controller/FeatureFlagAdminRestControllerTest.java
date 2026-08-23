package com.carland.carland_service.controller;

import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FeatureFlagAdminRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeatureFlagAdminRestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FeatureFlagService featureFlagService;

    @MockitoBean
    AdminAccessService adminAccessService;

    @Test
    void catalog_unauthorizedWithoutToken() throws Exception {
        when(adminAccessService.inspect(org.mockito.ArgumentMatchers.any()))
                .thenReturn(AdminAccessService.Status.MISSING);
        mockMvc.perform(get("/admin/endpoints"))
                .andExpect(status().isUnauthorized());
        verify(featureFlagService, never()).listEndpoints(anyInt(), anyInt());
    }

    @Test
    void catalog_forbiddenForNonAdmin() throws Exception {
        when(adminAccessService.inspect(org.mockito.ArgumentMatchers.any()))
                .thenReturn(AdminAccessService.Status.FORBIDDEN);
        mockMvc.perform(get("/admin/endpoints").header("Authorization", "Bearer x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void catalog_rejectsInvalidSize() throws Exception {
        when(adminAccessService.inspect(org.mockito.ArgumentMatchers.any()))
                .thenReturn(AdminAccessService.Status.OK);
        when(featureFlagService.listEndpoints(0, 15))
                .thenThrow(new IllegalArgumentException("size must be 10 or 25"));
        mockMvc.perform(get("/admin/endpoints").param("page", "0").param("size", "15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("size must be 10 or 25"));
    }

    @Test
    void catalog_returnsPageEnvelope() throws Exception {
        when(adminAccessService.inspect(org.mockito.ArgumentMatchers.any()))
                .thenReturn(AdminAccessService.Status.OK);
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("content", List.of(Map.of("method", "GET", "path", "/api/v1/x", "flagName", "TEST_FLOW")));
        page.put("page", 0);
        page.put("size", 10);
        page.put("totalElements", 1L);
        page.put("totalPages", 1);
        when(featureFlagService.listEndpoints(0, 10)).thenReturn(page);

        mockMvc.perform(get("/admin/endpoints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].flagName").value("TEST_FLOW"))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void globalAudit_passesFlagNameFilter() throws Exception {
        when(adminAccessService.inspect(org.mockito.ArgumentMatchers.any()))
                .thenReturn(AdminAccessService.Status.OK);
        when(featureFlagService.listAudit(eq(0), eq(25), eq("TEST_FLOW")))
                .thenReturn(Map.of("content", List.of(), "page", 0, "size", 25, "totalElements", 0L, "totalPages", 0));

        mockMvc.perform(get("/admin/audit").param("size", "25").param("flagName", "TEST_FLOW"))
                .andExpect(status().isOk());
        verify(featureFlagService).listAudit(0, 25, "TEST_FLOW");
    }
}
