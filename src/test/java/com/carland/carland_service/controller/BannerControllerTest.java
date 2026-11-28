package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.BannerItem;
import com.carland.carland_service.entity.Banner;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.security.CustomFilter;
import com.carland.carland_service.security.JWTConfiguration;
import com.carland.carland_service.service.AdminBannerService;
import com.carland.carland_service.service.FeatureFlagService;
import com.carland.carland_service.service.HyperBranchLockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import com.carland.carland_service.exceptions.CustomExceptionHandler;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BannerController.class)
@AutoConfigureMockMvc
@Import({JWTConfiguration.class, CustomFilter.class, CustomExceptionHandler.class})
class BannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminBannerService adminBannerService;

    @MockitoBean
    private HyperBranchLockService hyperBranchLockService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @MockitoBean
    private CustomerRepository customerRepository;

    @Test
    void emptyWithoutTokenIs200Array() throws Exception {
        when(adminBannerService.visible("az")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/banners"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("charset=UTF-8")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void brokenBearerStillReturnsTheBanner() throws Exception {
        when(adminBannerService.visible("az")).thenReturn(List.of(BannerItem.builder()
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

    @Test
    void oldHyperCarUsesTheHyperBannerList() throws Exception {
        when(hyperBranchLockService.locked(15L, 9L)).thenReturn(true);
        when(adminBannerService.visibleForOldHyper("az")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/banners")
                        .param("carId", "15")
                        .header("X-User-Id", "9"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(adminBannerService).visibleForOldHyper("az");
    }

    @Test
    void englishHeaderAsksForEnglishBanners() throws Exception {
        when(adminBannerService.visible("en")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/banners").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(adminBannerService).visible("en");
    }

    @Test
    void oneBannerReturnsTheSameObject() throws Exception {
        when(adminBannerService.visibleById(4L)).thenReturn(BannerItem.builder()
                .id(4L)
                .type(Banner.PROMO)
                .promoId(3L)
                .title("Yay")
                .description("endirim")
                .showing(true)
                .status("active")
                .build());
        when(customerRepository.findByUserId(9L)).thenReturn(Customer.builder().userId(9L).hasPromo(true).build());

        mockMvc.perform(get("/api/v1/banners/4").header("X-User-Id", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.promoId").value(3))
                .andExpect(jsonPath("$.type").value("promo"));
    }

    @Test
    void onePromoBannerIsHiddenWithoutHasPromo() throws Exception {
        when(adminBannerService.visibleById(4L)).thenReturn(BannerItem.builder()
                .id(4L)
                .type(Banner.PROMO)
                .title("Yay")
                .description("endirim")
                .showing(true)
                .status("active")
                .build());

        mockMvc.perform(get("/api/v1/banners/4"))
                .andExpect(status().isNotFound());
    }

    @Test
    void promoBannerIsHiddenWhenHasPromoIsFalse() throws Exception {
        when(adminBannerService.visible("az")).thenReturn(List.of(
                BannerItem.builder().id(1L).type(Banner.SERVICE).title("Servis").description("yag").showing(true).status("active").build(),
                BannerItem.builder().id(2L).type(Banner.PROMO).title("Promo").description("kod").showing(true).status("active").build()
        ));
        when(customerRepository.findByUserId(9L)).thenReturn(Customer.builder().userId(9L).hasPromo(false).build());

        mockMvc.perform(get("/api/v1/banners").header("X-User-Id", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("service"));
    }

    @Test
    void promoBannerStaysWhenHasPromoIsTrue() throws Exception {
        when(adminBannerService.visible("az")).thenReturn(List.of(
                BannerItem.builder().id(2L).type(Banner.PROMO).title("Promo").description("kod").showing(true).status("active").build()
        ));
        when(customerRepository.findByUserId(9L)).thenReturn(Customer.builder().userId(9L).hasPromo(true).build());

        mockMvc.perform(get("/api/v1/banners").header("X-User-Id", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("promo"));
    }
}
