package com.carland.carland_service.controller;

import com.carland.carland_service.digitalfinance.DigitalFinanceClient;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminIddaBalanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminIddaBalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @MockitoBean
    private DigitalFinanceClient digitalFinanceClient;

    @BeforeEach
    void panelAdmin() {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(true);
    }

    @Test
    void guestIsSentToLogin() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(get("/admin/idda-balance").accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://digital-innovation.agency/admin/"));

        verifyNoInteractions(digitalFinanceClient);
    }

    @Test
    void openingATabDoesNotCallUpstream() throws Exception {
        mockMvc.perform(get("/admin/idda-balance")
                        .param("tab", "expense")
                        .param("StartDate", "2026-10-01 00:00:00")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("iddaBalanceInfo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"RequestIdentifier\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"StartDate\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"EndDate\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"Offset\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"Limit\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"2026-10-01 00:00:00\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"ApiKey\""))));

        verifyNoInteractions(digitalFinanceClient);
    }

    @Test
    void balanceTabHasNoBodyFields() throws Exception {
        mockMvc.perform(get("/admin/idda-balance").param("tab", "balance").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GET /api/v1/Info/Balance")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Body yok")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"RequestIdentifier\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"startDate\""))));

        verifyNoInteractions(digitalFinanceClient);
    }

    @Test
    void expenseSendsOnlyFilledBodyFields() throws Exception {
        when(digitalFinanceClient.exchange(eq("POST"), eq("/api/v1/Info/Expenses"), any(byte[].class), eq("req-1"), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", expenseJson()));

        mockMvc.perform(post("/admin/idda-balance")
                        .param("tab", "expense")
                        .param("RequestIdentifier", "req-1")
                        .param("StartDate", "2026-10-01 00:00:00")
                        .param("Offset", "0")
                        .param("Limit", "20")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HTTP 200")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge-success")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("0 Successful")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("77BU669")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DIN.Vehicle")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">250<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">2.50<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10.1.1.1")));

        org.mockito.ArgumentCaptor<byte[]> body = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(digitalFinanceClient).exchange(eq("POST"), eq("/api/v1/Info/Expenses"), body.capture(), eq("req-1"), isNull());
        assertEquals(
                "{\"StartDate\":\"2026-10-01 00:00:00\",\"Offset\":0,\"Limit\":20}",
                new String(body.getValue(), StandardCharsets.UTF_8));
    }

    @Test
    void paymentUsesPaymentsPathAndLeavesAmountAsReturned() throws Exception {
        byte[] json = ("{\"RequestIdentifier\":\"pay-1\",\"Status\":{\"Name\":\"Successful\",\"Code\":0,\"Message\":\"\"},"
                + "\"Response\":{\"Count\":1,\"Payments\":[{\"Date\":\"2026-10-02 09:15:00\",\"Amount\":4321}]}}")
                .getBytes(StandardCharsets.UTF_8);
        when(digitalFinanceClient.exchange(eq("POST"), eq("/api/v1/Info/Payments"), any(byte[].class), eq("pay-1"), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", json));

        mockMvc.perform(post("/admin/idda-balance")
                        .param("tab", "payment")
                        .param("RequestIdentifier", "pay-1")
                        .param("EndDate", "2026-10-21 23:59:59")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2026-10-02 09:15:00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">4321<")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Amount (qəpik)"))));

        org.mockito.ArgumentCaptor<byte[]> body = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(digitalFinanceClient).exchange(eq("POST"), eq("/api/v1/Info/Payments"), body.capture(), eq("pay-1"), isNull());
        assertEquals("{\"EndDate\":\"2026-10-21 23:59:59\"}", new String(body.getValue(), StandardCharsets.UTF_8));
    }

    @Test
    void balanceIsGetAndKeepsTypedDatesOffTheUpstreamBody() throws Exception {
        byte[] json = ("{\"RequestIdentifier\":\"bal-1\",\"Status\":{\"Name\":\"Successful\",\"Code\":0,\"Message\":\"\"},"
                + "\"Response\":{\"Balance\":15000}}").getBytes(StandardCharsets.UTF_8);
        when(digitalFinanceClient.exchange(eq("GET"), eq("/api/v1/Info/Balance"), isNull(), eq("bal-1"), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", json));

        mockMvc.perform(post("/admin/idda-balance")
                        .param("tab", "balance")
                        .param("RequestIdentifier", "bal-1")
                        .param("StartDate", "2026-10-01 00:00:00")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">15000<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("150.00")));

        verify(digitalFinanceClient).exchange(eq("GET"), eq("/api/v1/Info/Balance"), isNull(), eq("bal-1"), isNull());
    }

    @Test
    void blankRequestIdentifierIsGeneratedAndShown() throws Exception {
        when(digitalFinanceClient.exchange(eq("GET"), eq("/api/v1/Info/Balance"), isNull(), anyString(), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json",
                        "{\"Status\":{\"Code\":0,\"Name\":\"Successful\",\"Message\":\"\"},\"Response\":{\"Balance\":0}}"
                                .getBytes(StandardCharsets.UTF_8)));

        String html = mockMvc.perform(post("/admin/idda-balance").param("tab", "balance").accept("text/html"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.mockito.ArgumentCaptor<String> requestId = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(digitalFinanceClient).exchange(eq("GET"), eq("/api/v1/Info/Balance"), isNull(), requestId.capture(), isNull());
        assertTrue(requestId.getValue().matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertEquals(requestId.getValue(), UUID.fromString(requestId.getValue()).toString());
        assertTrue(html.contains("value=\"" + requestId.getValue() + "\""));
    }

    @Test
    void invalidOffsetDoesNotCallUpstream() throws Exception {
        mockMvc.perform(post("/admin/idda-balance")
                        .param("tab", "expense")
                        .param("Offset", "1.5")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Offset tam sayı olmalı.")));

        verifyNoInteractions(digitalFinanceClient);
    }

    @Test
    void upstreamFailureStaysOnTheForm() throws Exception {
        when(digitalFinanceClient.exchange(anyString(), anyString(), nullable(byte[].class), anyString(), nullable(String.class)))
                .thenThrow(new IllegalStateException("DIGITAL_FINANCE_API_KEY is empty"));

        mockMvc.perform(post("/admin/idda-balance").param("tab", "balance").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DIGITAL_FINANCE_API_KEY is empty")));
    }

    @Test
    void nonJsonBodyIsShownRaw() throws Exception {
        when(digitalFinanceClient.exchange(eq("GET"), eq("/api/v1/Info/Balance"), isNull(), anyString(), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(502, "text/plain", "not-json".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(post("/admin/idda-balance").param("tab", "balance").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HTTP 502")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("not-json")));
    }

    private static byte[] expenseJson() {
        return ("{\"RequestIdentifier\":\"req-1\",\"Status\":{\"Name\":\"Successful\",\"Code\":0,\"Message\":\"\"},"
                + "\"Response\":{\"Count\":1,\"Expenses\":[{\"Value\":\"77BU669\",\"Date\":\"2026-10-03 14:00:00\","
                + "\"Service\":\"DIN.Vehicle\",\"Amount\":250,\"RequestIdentifier\":\"car-1\",\"IpAddress\":\"10.1.1.1\"}]}}")
                .getBytes(StandardCharsets.UTF_8);
    }
}
