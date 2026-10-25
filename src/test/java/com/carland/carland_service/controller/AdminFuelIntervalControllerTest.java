package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.AdminFuelCatalogOption;
import com.carland.carland_service.dto.response.AdminFuelIntervalPage;
import com.carland.carland_service.dto.response.AdminFuelIntervalRow;
import com.carland.carland_service.dto.response.AdminFuelTypeOption;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.AdminFuelIntervalService;
import com.carland.carland_service.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminFuelIntervalController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminFuelIntervalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private AdminFuelIntervalService adminFuelIntervalService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @Test
    void guestIsSentToLogin() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(get("/admin/fuel-intervals").accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://digital-innovation.agency/admin/"));

        verify(adminFuelIntervalService, never()).open(any());
    }

    @Test
    void panelAdminGetsTheFuelSwitchAndTheIntervalTable() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(true);
        when(adminFuelIntervalService.open(any())).thenReturn(AdminFuelIntervalPage.builder()
                .fuels(List.of(AdminFuelTypeOption.builder().engineTypeId(2L).label("Hibrid").build()))
                .selectedEngineTypeId(2L)
                .selectedLabel("Hibrid")
                .templateMissing(false)
                .templateName("Hybrid")
                .rows(List.of(AdminFuelIntervalRow.builder()
                        .serviceId(9L).titleAz("Buji").titleEn("Spark plugs")
                        .intervalKm(20_000L).intervalMonth(24).otherFuels("Dizel").build()))
                .catalogOptions(List.of(AdminFuelCatalogOption.builder().id(8L).code("YAG").titleAz("Yağ").build()))
                .build());

        mockMvc.perform(get("/admin/fuel-intervals").param("engineTypeId", "2").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Yakıt aralıqları")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hibrid")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bu yakıta əlavə et")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Buji")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("20000")));
    }

    @Test
    void intervalPostRequiresThePanelToken() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(post("/admin/fuel-intervals/interval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":9,\"intervalKm\":15000}"))
                .andExpect(status().isUnauthorized());

        verify(adminFuelIntervalService, never()).updateInterval(any());
    }
}
