package com.carland.carland_service.controller;

import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.AdminPromoService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminPromoController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPromoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private AdminPromoService adminPromoService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @Test
    void guestIsSentToLogin() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(get("/admin/promos").accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://digital-innovation.agency/admin/"));
    }

    @Test
    void panelAdminGetsThePage() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(true);

        mockMvc.perform(get("/admin/promos").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">promo code<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"createBox\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Düzəlt")));
    }

    @Test
    void createRequiresPanelToken() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(post("/admin/promos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\"}"))
                .andExpect(status().isUnauthorized());

        verify(adminPromoService, never()).create(any());
    }
}
