package com.carland.carland_service.controller;

import com.carland.carland_service.idda.IddaPlateCheck;
import com.carland.carland_service.idda.IddaPlateCheckRepository;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminIddaPlateCheckController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminIddaPlateCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @MockitoBean
    private IddaPlateCheckRepository iddaPlateCheckRepository;

    @Test
    void guestIsSentToLogin() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(get("/admin/idda").accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://digital-innovation.agency/admin/"));

        verify(iddaPlateCheckRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void panelAdminSeesSuccessAndFailureRows() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(true);
        when(iddaPlateCheckRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(success(), failure())));

        mockMvc.perform(get("/admin/idda").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">IDDA<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"customerId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"plate\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"vin\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("77 BU 669")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("19XZE4F54NE012640")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge-success")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("0 Successful")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge-danger")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("4 ServiceRestrictedByPerson")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("09:41")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">Yes<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">No<")));
    }

    @Test
    void filtersStayInTheForm() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(true);
        when(iddaPlateCheckRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/idda")
                        .param("customerId", "7")
                        .param("plate", "77-BU-669")
                        .param("vin", "19xze")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"7\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"77-BU-669\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"19xze\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kayıt bulunamadı")));
    }

    private static IddaPlateCheck success() {
        return IddaPlateCheck.builder()
                .id(101L)
                .plateKey("77BU669")
                .plateNumber("77BU669")
                .vin("19XZE4F54NE012640")
                .brand("Honda")
                .model("Insight")
                .modelYear(2022)
                .colorRaw("Gümüşü metallik")
                .statusCode(0)
                .statusName("Successful")
                .querierUserId(7L)
                .querierName("A")
                .querierSurname("B")
                .queriedAt(LocalDateTime.of(2026, 10, 29, 5, 41))
                .countsTowardLimit(true)
                .build();
    }

    private static IddaPlateCheck failure() {
        return IddaPlateCheck.builder()
                .id(102L)
                .plateKey("10BB100")
                .plateNumber("10BB100")
                .statusCode(4)
                .statusName("ServiceRestrictedByPerson")
                .statusMessage("limit")
                .querierUserId(7L)
                .queriedAt(LocalDateTime.of(2026, 10, 29, 5, 42))
                .countsTowardLimit(false)
                .build();
    }
}
