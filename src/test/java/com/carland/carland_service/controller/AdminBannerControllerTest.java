package com.carland.carland_service.controller;

import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.AdminBannerService;
import com.carland.carland_service.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminBannerController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminBannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private AdminBannerService adminBannerService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @Test
    void guestIsSentToLogin() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(get("/admin/banners").accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://digital-innovation.agency/admin/"));
    }

    @Test
    void panelAdminGetsThePage() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(true);

        mockMvc.perform(get("/admin/banners").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">banner<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"createBtn\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">branchId<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">showing<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">status<")));
    }

    @Test
    void createRequiresPanelToken() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(post("/admin/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A\",\"description\":\"abc\",\"branchId\":1}"))
                .andExpect(status().isUnauthorized());

        verify(adminBannerService, never()).create(any());
    }

    @Test
    void showingPutReachesTheEdit() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(true);

        mockMvc.perform(put("/admin/banners/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showing\":true}"))
                .andExpect(status().isOk());

        verify(adminBannerService).update(eq(5L), any());
    }
}
