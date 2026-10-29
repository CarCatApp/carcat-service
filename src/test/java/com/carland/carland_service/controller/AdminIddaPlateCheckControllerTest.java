package com.carland.carland_service.controller;

import com.carland.carland_service.idda.IddaPlateCheck;
import com.carland.carland_service.idda.IddaPlateCheckRepository;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.FeatureFlagService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">No<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/idda/export\"")));
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kayıt bulunamadı")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "href=\"/admin/idda/export?customerId=7&amp;plate=77-BU-669&amp;vin=19xze\"")));
    }

    @Test
    void guestExportIsSentToLogin() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(false);

        mockMvc.perform(get("/admin/idda/export"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://digital-innovation.agency/admin/"));

        verify(iddaPlateCheckRepository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void exportWritesEveryStoredColumn() throws Exception {
        when(adminAccessService.isPanelAdmin(any())).thenReturn(true);
        IddaPlateCheck row = success();
        row.setRequestIdentifier("req-1");
        row.setRawMark("HONDA INSIGHT");
        row.setEngineCapacity(1498);
        row.setEngineNumber("ENG1");
        row.setVehicleTypeId(1);
        row.setVehicleTypeDescription("Avtomobil");
        row.setCertificateNumber("CERT");
        row.setCertificateGivenDate("2022-01-02");
        row.setOwnerName("Ali");
        row.setOwnerSurname("Veli");
        row.setOwnerPatronymic("Oglu");
        row.setRawJson("{\"ok\":true}");
        when(iddaPlateCheckRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(row));

        MvcResult result = mockMvc.perform(get("/admin/idda/export")
                        .param("customerId", "7")
                        .param("plate", "77-BU-669")
                        .param("vin", "19xze"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("idda-filtered-")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andReturn();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            String[] expected = {
                    "ID", "Queried at", "Customer ID", "Querier name", "Querier surname",
                    "Plate", "Plate key", "VIN", "Status code", "Status name", "Message", "Limit",
                    "Request identifier", "Brand", "Model", "Year", "Color", "Raw mark",
                    "Engine capacity", "Engine number", "Vehicle type ID", "Vehicle type",
                    "Certificate number", "Certificate given date",
                    "Owner name", "Owner surname", "Owner patronymic", "Raw JSON"
            };
            assertEquals(expected.length, header.getLastCellNum());
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], header.getCell(i).getStringCellValue());
            }
            Row data = sheet.getRow(1);
            assertEquals(101d, data.getCell(0).getNumericCellValue());
            assertEquals("2026-10-29 09:41", data.getCell(1).getStringCellValue());
            assertEquals(7d, data.getCell(2).getNumericCellValue());
            assertEquals("A", data.getCell(3).getStringCellValue());
            assertEquals("B", data.getCell(4).getStringCellValue());
            assertEquals("77 BU 669", data.getCell(5).getStringCellValue());
            assertEquals("77BU669", data.getCell(6).getStringCellValue());
            assertEquals("19XZE4F54NE012640", data.getCell(7).getStringCellValue());
            assertEquals(0d, data.getCell(8).getNumericCellValue());
            assertEquals("Successful", data.getCell(9).getStringCellValue());
            assertEquals("Yes", data.getCell(11).getStringCellValue());
            assertEquals("req-1", data.getCell(12).getStringCellValue());
            assertEquals("Honda", data.getCell(13).getStringCellValue());
            assertEquals(2022d, data.getCell(15).getNumericCellValue());
            assertEquals("Gümüşü metallik", data.getCell(16).getStringCellValue());
            assertEquals("HONDA INSIGHT", data.getCell(17).getStringCellValue());
            assertEquals(1498d, data.getCell(18).getNumericCellValue());
            assertEquals("Ali", data.getCell(24).getStringCellValue());
            assertEquals("Oglu", data.getCell(26).getStringCellValue());
            assertEquals("{\"ok\":true}", data.getCell(27).getStringCellValue());
        }
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
