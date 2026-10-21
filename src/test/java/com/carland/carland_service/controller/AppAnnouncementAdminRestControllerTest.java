package com.carland.carland_service.controller;

import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.AppAnnouncementService;
import com.carland.carland_service.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppAnnouncementAdminRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppAnnouncementAdminRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppAnnouncementService appAnnouncementService;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @Test
    void listRequiresPanelToken() throws Exception {
        when(adminAccessService.inspect(any())).thenReturn(AdminAccessService.Status.MISSING);

        mockMvc.perform(get("/admin/app-announcements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("JWT token required"));
        verify(appAnnouncementService, never()).list(any(), any(), any());
    }

    @Test
    void otherJwtIsForbidden() throws Exception {
        when(adminAccessService.inspect(any())).thenReturn(AdminAccessService.Status.FORBIDDEN);

        mockMvc.perform(get("/admin/app-announcements/4"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin not found"));
    }

    @Test
    void invalidFilterIs400() throws Exception {
        when(adminAccessService.inspect(any())).thenReturn(AdminAccessService.Status.OK);

        mockMvc.perform(get("/admin/app-announcements").param("active", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("active must be true or false"));
        verify(appAnnouncementService, never()).list(any(), any(), any());
    }

    @Test
    void previewRejectsBadInstant() throws Exception {
        when(adminAccessService.inspect(any())).thenReturn(AdminAccessService.Status.OK);

        mockMvc.perform(get("/admin/app-announcements/preview").param("at", "tomorrow"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("at must be ISO-8601 UTC"));
    }

    @Test
    void publishRequiresActiveFlag() throws Exception {
        when(adminAccessService.inspect(any())).thenReturn(AdminAccessService.Status.OK);

        mockMvc.perform(post("/admin/app-announcements/3/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("active is required"));
    }
}
