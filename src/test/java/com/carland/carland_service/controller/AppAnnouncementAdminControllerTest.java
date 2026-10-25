package com.carland.carland_service.controller;

import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppAnnouncementAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppAnnouncementAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @Test
    void guestIsSentToLogin() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(get("/admin/app-announcements").accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://digital-innovation.agency/admin/"));
    }

    @Test
    void panelAdminGetsThePage() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(true);

        mockMvc.perform(get("/admin/app-announcements").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Duyurular")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Telefonda ne görünür")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CTA URL iOS")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CTA URL Android")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"publishBtn\"")));
    }
}
