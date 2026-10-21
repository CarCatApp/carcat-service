package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.AppAnnouncementActiveItem;
import com.carland.carland_service.security.CustomFilter;
import com.carland.carland_service.security.JWTConfiguration;
import com.carland.carland_service.service.AppAnnouncementService;
import com.carland.carland_service.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppAnnouncementController.class)
@AutoConfigureMockMvc
@Import({JWTConfiguration.class, CustomFilter.class})
class AppAnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppAnnouncementService appAnnouncementService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @Test
    void emptyWithoutHeadersIs200Array() throws Exception {
        when(appAnnouncementService.active(nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/app-announcements/active"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("charset=UTF-8")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=60")))
                .andExpect(header().string("Vary", org.hamcrest.Matchers.containsString("X-App-Version")));
    }

    @Test
    void brokenBearerStillReturns200() throws Exception {
        when(appAnnouncementService.active(nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(List.of(AppAnnouncementActiveItem.builder()
                        .id(12L)
                        .type("UPDATE")
                        .dismissible(false)
                        .title("Şəhər")
                        .body("Yenilə")
                        .ctaLabel("Yenilə")
                        .ctaUrl("https://apps.apple.com/app/id1")
                        .build()));

        mockMvc.perform(get("/api/v1/app-announcements/active")
                        .header("Accept-Language", "az")
                        .header("X-App-Version", "3.0.5")
                        .header("X-Platform", "IOS")
                        .header("Authorization", "Bearer bozuk.token.degeri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].title").value("Şəhər"))
                .andExpect(jsonPath("$[0].minAppVersion").doesNotExist())
                .andExpect(jsonPath("$[0].platform").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    @Test
    void failureIs500WithMessage() throws Exception {
        when(appAnnouncementService.active(nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/api/v1/app-announcements/active"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unable to load announcements"));
    }
}
