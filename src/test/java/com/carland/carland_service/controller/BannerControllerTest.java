package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.BannerItem;
import com.carland.carland_service.security.CustomFilter;
import com.carland.carland_service.security.JWTConfiguration;
import com.carland.carland_service.service.AdminBannerService;
import com.carland.carland_service.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BannerController.class)
@AutoConfigureMockMvc
@Import({JWTConfiguration.class, CustomFilter.class})
class BannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminBannerService adminBannerService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @Test
    void emptyWithoutTokenIs200Array() throws Exception {
        when(adminBannerService.visible()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/banners"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("charset=UTF-8")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void brokenBearerStillReturnsTheBanner() throws Exception {
        when(adminBannerService.visible()).thenReturn(List.of(BannerItem.builder()
                .id(4L)
                .title("hyper babek prospekti")
                .description("Yağ dəyişimi")
                .branchId(12L)
                .showing(true)
                .status("active")
                .build()));

        mockMvc.perform(get("/api/v1/banners")
                        .header("Authorization", "Bearer bozuk.token.degeri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].title").value("hyper babek prospekti"))
                .andExpect(jsonPath("$[0].description").value("Yağ dəyişimi"))
                .andExpect(jsonPath("$[0].branchId").value(12))
                .andExpect(jsonPath("$[0].showing").value(true))
                .andExpect(jsonPath("$[0].status").value("active"));
    }
}
