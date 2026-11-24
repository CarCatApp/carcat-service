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

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"date\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"Offset\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"Limit\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sayfa başı")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"2026-10-01\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("value=\"2026-10-01 00:00:00\""))))
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
        assertEquals(
                "{\"EndDate\":\"2026-10-21 23:59:59\",\"Offset\":0,\"Limit\":50}",
                new String(body.getValue(), StandardCharsets.UTF_8));
    }

    @Test
    void calendarDaysAreSentAsStartOfDayAndEndOfDay() throws Exception {
        when(digitalFinanceClient.exchange(eq("POST"), eq("/api/v1/Info/Payments"), any(byte[].class), eq("pay-2"), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", counted(1)));

        mockMvc.perform(post("/admin/idda-balance")
                        .param("tab", "payment")
                        .param("RequestIdentifier", "pay-2")
                        .param("nav", "search")
                        .param("StartDate", "2026-10-01")
                        .param("EndDate", "2026-10-02")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"2026-10-01\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"2026-10-02\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1–1 / 1")));

        org.mockito.ArgumentCaptor<byte[]> body = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(digitalFinanceClient).exchange(eq("POST"), eq("/api/v1/Info/Payments"), body.capture(), eq("pay-2"), isNull());
        assertEquals(
                "{\"StartDate\":\"2026-10-01 00:00:00\",\"EndDate\":\"2026-10-02 23:59:59\",\"Offset\":0,\"Limit\":50}",
                new String(body.getValue(), StandardCharsets.UTF_8));
    }

    @Test
    void nextAndPreviousMoveByPageSize() throws Exception {
        when(digitalFinanceClient.exchange(eq("POST"), eq("/api/v1/Info/Payments"), any(byte[].class), anyString(), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", counted(120)));

        mockMvc.perform(post("/admin/idda-balance")
                        .param("tab", "payment")
                        .param("nav", "next")
                        .param("StartDate", "2026-10-01")
                        .param("EndDate", "2026-10-02")
                        .param("Offset", "0")
                        .param("Limit", "50")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("51–100 / 120")));

        org.mockito.ArgumentCaptor<byte[]> nextBody = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(digitalFinanceClient).exchange(eq("POST"), eq("/api/v1/Info/Payments"), nextBody.capture(), anyString(), isNull());
        assertTrue(new String(nextBody.getValue(), StandardCharsets.UTF_8).contains("\"Offset\":50"));

        mockMvc.perform(post("/admin/idda-balance")
                        .param("tab", "payment")
                        .param("nav", "prev")
                        .param("StartDate", "2026-10-01")
                        .param("EndDate", "2026-10-02")
                        .param("Offset", "100")
                        .param("Limit", "50")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("51–100 / 120")));

        org.mockito.ArgumentCaptor<byte[]> prevBody = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(digitalFinanceClient, org.mockito.Mockito.times(2))
                .exchange(eq("POST"), eq("/api/v1/Info/Payments"), prevBody.capture(), anyString(), isNull());
        assertTrue(new String(prevBody.getAllValues().get(1), StandardCharsets.UTF_8).contains("\"Offset\":50"));
    }

    @Test
    void searchStartsAtTheFirstPage() throws Exception {
        when(digitalFinanceClient.exchange(eq("POST"), eq("/api/v1/Info/Expenses"), any(byte[].class), anyString(), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", expenseJson()));

        mockMvc.perform(post("/admin/idda-balance")
                        .param("tab", "expense")
                        .param("nav", "search")
                        .param("StartDate", "2026-10-01")
                        .param("Offset", "50")
                        .param("Limit", "20")
                        .accept("text/html"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<byte[]> body = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(digitalFinanceClient).exchange(eq("POST"), eq("/api/v1/Info/Expenses"), body.capture(), anyString(), isNull());
        assertEquals(
                "{\"StartDate\":\"2026-10-01 00:00:00\",\"Offset\":0,\"Limit\":20}",
                new String(body.getValue(), StandardCharsets.UTF_8));
    }

    @Test
    void endDayBeforeStartDoesNotCallUpstream() throws Exception {
        mockMvc.perform(post("/admin/idda-balance")
                        .param("tab", "payment")
                        .param("StartDate", "2026-10-02")
                        .param("EndDate", "2026-10-01")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bitiş, başlangıçtan önce olamaz.")));

        verifyNoInteractions(digitalFinanceClient);
    }

    @Test
    void pageSizeOutsideTheChoicesDoesNotCallUpstream() throws Exception {
        mockMvc.perform(post("/admin/idda-balance")
                        .param("tab", "payment")
                        .param("Limit", "15")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sayfa başı 20, 50 veya 100 olmalı.")));

        verifyNoInteractions(digitalFinanceClient);
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
                        .param("nav", "next")
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
    void pageOffersAnExcelDownload() throws Exception {
        mockMvc.perform(get("/admin/idda-balance").param("tab", "expense").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("formaction=\"/admin/idda-balance/export\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Excel")));

        verifyNoInteractions(digitalFinanceClient);
    }

    @Test
    void expenseExcelContainsEveryTableFieldAndExtraJsonFields() throws Exception {
        when(digitalFinanceClient.exchange(eq("POST"), eq("/api/v1/Info/Expenses"), any(byte[].class), eq("req-x"), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", expensePage(2, "77BU669", "alpha")),
                        new DigitalFinanceClient.Upstream(200, "application/json", expensePage(2, "10AA010", "beta")));

        byte[] file = mockMvc.perform(post("/admin/idda-balance/export")
                        .param("tab", "expense")
                        .param("RequestIdentifier", "req-x")
                        .param("StartDate", "2026-10-01")
                        .param("EndDate", "2026-10-03")
                        .param("Offset", "50")
                        .param("Limit", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("idda-expense-")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        org.mockito.ArgumentCaptor<byte[]> body = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(digitalFinanceClient, org.mockito.Mockito.times(2))
                .exchange(eq("POST"), eq("/api/v1/Info/Expenses"), body.capture(), eq("req-x"), isNull());
        assertEquals(
                "{\"StartDate\":\"2026-10-01 00:00:00\",\"EndDate\":\"2026-10-03 23:59:59\",\"Offset\":0,\"Limit\":100}",
                new String(body.getAllValues().get(0), StandardCharsets.UTF_8));
        assertTrue(new String(body.getAllValues().get(1), StandardCharsets.UTF_8).contains("\"Offset\":1"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            Sheet sheet = workbook.getSheet("Expense");
            Row header = sheet.getRow(0);
            assertEquals("Value", header.getCell(0).getStringCellValue());
            assertEquals("Date", header.getCell(1).getStringCellValue());
            assertEquals("Service", header.getCell(2).getStringCellValue());
            assertEquals("Amount (qəpik)", header.getCell(3).getStringCellValue());
            assertEquals("AZN", header.getCell(4).getStringCellValue());
            assertEquals("RequestIdentifier", header.getCell(5).getStringCellValue());
            assertEquals("IpAddress", header.getCell(6).getStringCellValue());
            assertEquals("Note", header.getCell(7).getStringCellValue());
            assertEquals("77BU669", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("2026-10-03 14:00:00", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(250d, sheet.getRow(1).getCell(3).getNumericCellValue(), 0.001);
            assertEquals(2.50d, sheet.getRow(1).getCell(4).getNumericCellValue(), 0.001);
            assertEquals("alpha", sheet.getRow(1).getCell(7).getStringCellValue());
            assertEquals("10AA010", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("beta", sheet.getRow(2).getCell(7).getStringCellValue());
        }
    }

    @Test
    void paymentExcelKeepsAmountAsReturned() throws Exception {
        byte[] json = ("{\"Status\":{\"Name\":\"Successful\",\"Code\":0,\"Message\":\"\"},"
                + "\"Response\":{\"Count\":1,\"Payments\":[{\"Date\":\"2026-10-02 09:15:00\",\"Amount\":4321}]}}")
                .getBytes(StandardCharsets.UTF_8);
        when(digitalFinanceClient.exchange(eq("POST"), eq("/api/v1/Info/Payments"), any(byte[].class), eq("pay-x"), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", json));

        byte[] file = mockMvc.perform(post("/admin/idda-balance/export")
                        .param("tab", "payment")
                        .param("RequestIdentifier", "pay-x")
                        .param("EndDate", "2026-10-21"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("idda-payment-")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            Sheet sheet = workbook.getSheet("Payment");
            assertEquals("Date", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Amount", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("2026-10-02 09:15:00", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(4321d, sheet.getRow(1).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(2, sheet.getRow(0).getLastCellNum());
        }
    }

    @Test
    void balanceExcelContainsBalanceFields() throws Exception {
        byte[] json = ("{\"RequestIdentifier\":\"bal-1\",\"Status\":{\"Name\":\"Successful\",\"Code\":0,\"Message\":\"ok\"},"
                + "\"Response\":{\"Balance\":15000}}").getBytes(StandardCharsets.UTF_8);
        when(digitalFinanceClient.exchange(eq("GET"), eq("/api/v1/Info/Balance"), isNull(), eq("bal-1"), isNull()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", json));

        byte[] file = mockMvc.perform(post("/admin/idda-balance/export")
                        .param("tab", "balance")
                        .param("RequestIdentifier", "bal-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("idda-balance-")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            Sheet sheet = workbook.getSheet("Balance");
            assertEquals("Balance (qəpik)", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("AZN", sheet.getRow(0).getCell(2).getStringCellValue());
            assertEquals("bal-1", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(15000d, sheet.getRow(1).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(150d, sheet.getRow(1).getCell(2).getNumericCellValue(), 0.001);
            assertEquals("Successful", sheet.getRow(1).getCell(4).getStringCellValue());
        }
    }

    @Test
    void exportGuestIsSentToLogin() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(post("/admin/idda-balance/export").param("tab", "expense"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://digital-innovation.agency/admin/"));

        verifyNoInteractions(digitalFinanceClient);
    }

    @Test
    void exportWithEndBeforeStartDoesNotCallUpstream() throws Exception {
        mockMvc.perform(post("/admin/idda-balance/export")
                        .param("tab", "expense")
                        .param("StartDate", "2026-10-02")
                        .param("EndDate", "2026-10-01")
                        .accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bitiş, başlangıçtan önce olamaz.")));

        verifyNoInteractions(digitalFinanceClient);
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

    private static byte[] counted(long count) {
        return ("{\"Status\":{\"Name\":\"Successful\",\"Code\":0,\"Message\":\"\"},\"Response\":{\"Count\":"
                + count + ",\"Payments\":[]}}").getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] expensePage(long count, String value, String note) {
        return ("{\"RequestIdentifier\":\"req-x\",\"Status\":{\"Name\":\"Successful\",\"Code\":0,\"Message\":\"\"},"
                + "\"Response\":{\"Count\":" + count + ",\"Expenses\":[{\"Value\":\"" + value
                + "\",\"Date\":\"2026-10-03 14:00:00\",\"Service\":\"DIN.Vehicle\",\"Amount\":250,"
                + "\"RequestIdentifier\":\"car-1\",\"IpAddress\":\"10.1.1.1\",\"Note\":\"" + note + "\"}]}}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] expenseJson() {
        return ("{\"RequestIdentifier\":\"req-1\",\"Status\":{\"Name\":\"Successful\",\"Code\":0,\"Message\":\"\"},"
                + "\"Response\":{\"Count\":1,\"Expenses\":[{\"Value\":\"77BU669\",\"Date\":\"2026-10-03 14:00:00\","
                + "\"Service\":\"DIN.Vehicle\",\"Amount\":250,\"RequestIdentifier\":\"car-1\",\"IpAddress\":\"10.1.1.1\"}]}}")
                .getBytes(StandardCharsets.UTF_8);
    }
}
