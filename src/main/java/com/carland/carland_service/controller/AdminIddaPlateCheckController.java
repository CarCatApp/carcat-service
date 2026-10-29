package com.carland.carland_service.controller;

import com.carland.carland_service.idda.IddaPlateCheck;
import com.carland.carland_service.idda.IddaPlateCheckRepository;
import com.carland.carland_service.idda.IddaPlateCheckSpec;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.util.PlateNumbers;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * tr: Admin IDDA plaka sorgu listesi. Redis yok, tablo idda_plate_checks.
 * en: Admin IDDA plate-check list. No Redis. Table idda_plate_checks.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminIddaPlateCheckController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";
    private static final int PAGE_SIZE = 10;
    private static final int EXCEL_CELL_MAX = 32767;
    private static final DateTimeFormatter EXCEL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "queriedAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));
    private static final String[] EXPORT_HEADERS = {
            "ID",
            "Queried at",
            "Customer ID",
            "Querier name",
            "Querier surname",
            "Plate",
            "Plate key",
            "VIN",
            "Status code",
            "Status name",
            "Message",
            "Limit",
            "Request identifier",
            "Brand",
            "Model",
            "Year",
            "Color",
            "Raw mark",
            "Engine capacity",
            "Engine number",
            "Vehicle type ID",
            "Vehicle type",
            "Certificate number",
            "Certificate given date",
            "Owner name",
            "Owner surname",
            "Owner patronymic",
            "Raw JSON"
    };

    private final AdminAccessService adminAccessService;
    private final IddaPlateCheckRepository iddaPlateCheckRepository;

    /**
     * tr: Tüm IDDA kayıtları, yeni üstte. Filtre: customerId, plaka, VIN.
     * en: Every IDDA row, newest first. Filters: customerId, plate, VIN.
     */
    @GetMapping("/admin/idda")
    public String page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) String vin,
            HttpServletRequest request,
            Model model
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }

        int pageIndex = Math.max(page, 1) - 1;
        Filters filters = filters(customerId, plate, vin);
        Pageable pageable = PageRequest.of(pageIndex, PAGE_SIZE, NEWEST_FIRST);
        Page<IddaPlateCheck> checks = iddaPlateCheckRepository.findAll(filters.spec(), pageable);

        addPagination(model, checks.getTotalPages(), pageIndex);
        model.addAttribute("checks", checks);
        model.addAttribute("filterCustomerId", filters.customerRaw());
        model.addAttribute("filterPlate", filters.plateRaw());
        model.addAttribute("filterVin", filters.vinRaw());
        model.addAttribute("iddaExportHref", exportHref(filters));
        return "idda";
    }

    /**
     * tr: Filtreli tüm satırlar, sayfa dilimi yok. Tabloda birleşen ad ve durum alanları ayrı kolon.
     * en: Every matching row, not the current page. Name and status fields that the table joins stay separate columns.
     */
    @GetMapping("/admin/idda/export")
    public void export(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) String vin,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (!adminAccessService.isPanelAdmin(request)) {
            response.sendRedirect(ADMIN_URL + "/admin/");
            return;
        }

        Filters filters = filters(customerId, plate, vin);
        List<IddaPlateCheck> rows = iddaPlateCheckRepository.findAll(filters.spec(), NEWEST_FIRST);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("IDDA");
            createHeaderRow(workbook, sheet);
            int rowIndex = 1;
            for (IddaPlateCheck row : rows) {
                writeRow(sheet.createRow(rowIndex++), row);
            }
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 60 * 256) {
                    sheet.setColumnWidth(i, 60 * 256);
                }
            }
            String baseName = filters.active() ? "idda-filtered" : "idda";
            String fileName = baseName + "-" + LocalDateTime.now().format(FILE_STAMP) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            workbook.write(response.getOutputStream());
        }
    }

    private static String exportHref(Filters filters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/idda/export");
        if (filters.customerRaw() != null) {
            builder.queryParam("customerId", filters.customerRaw());
        }
        if (filters.plateRaw() != null) {
            builder.queryParam("plate", filters.plateRaw());
        }
        if (filters.vinRaw() != null) {
            builder.queryParam("vin", filters.vinRaw());
        }
        return builder.build().encode().toUriString();
    }

    private static Filters filters(String customerId, String plate, String vin) {
        String customerRaw = blankToNull(customerId);
        Long customerFilter = null;
        boolean customerRejected = false;
        if (customerRaw != null) {
            try {
                customerFilter = Long.valueOf(customerRaw);
            } catch (NumberFormatException ex) {
                customerRejected = true;
            }
        }
        return new Filters(customerFilter, customerRejected, customerRaw, blankToNull(plate), blankToNull(vin));
    }

    private static void createHeaderRow(Workbook workbook, Sheet sheet) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < EXPORT_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(EXPORT_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private static void writeRow(Row row, IddaPlateCheck check) {
        setNumericCell(row, 0, check.getId());
        setCell(row, 1, formatQueriedAt(check.getQueriedAt()));
        setNumericCell(row, 2, check.getQuerierUserId());
        setCell(row, 3, check.getQuerierName());
        setCell(row, 4, check.getQuerierSurname());
        String plate = check.getPlateNumber() != null ? check.getPlateNumber() : check.getPlateKey();
        setCell(row, 5, PlateNumbers.format(plate));
        setCell(row, 6, check.getPlateKey());
        setCell(row, 7, check.getVin());
        setNumericCell(row, 8, check.getStatusCode());
        setCell(row, 9, check.getStatusName());
        setCell(row, 10, check.getStatusMessage());
        setCell(row, 11, check.isCountsTowardLimit() ? "Yes" : "No");
        setCell(row, 12, check.getRequestIdentifier());
        setCell(row, 13, check.getBrand());
        setCell(row, 14, check.getModel());
        setNumericCell(row, 15, check.getModelYear());
        setCell(row, 16, check.getColorRaw());
        setCell(row, 17, check.getRawMark());
        setNumericCell(row, 18, check.getEngineCapacity());
        setCell(row, 19, check.getEngineNumber());
        setNumericCell(row, 20, check.getVehicleTypeId());
        setCell(row, 21, check.getVehicleTypeDescription());
        setCell(row, 22, check.getCertificateNumber());
        setCell(row, 23, check.getCertificateGivenDate());
        setCell(row, 24, check.getOwnerName());
        setCell(row, 25, check.getOwnerSurname());
        setCell(row, 26, check.getOwnerPatronymic());
        setCell(row, 27, check.getRawJson());
    }

    private static String formatQueriedAt(LocalDateTime queriedAt) {
        return queriedAt != null ? EXCEL_DATE_FORMAT.format(queriedAt.plusHours(4)) : "";
    }

    private static void setCell(Row row, int column, String value) {
        String text = value == null ? "" : value;
        if (text.length() > EXCEL_CELL_MAX) {
            text = text.substring(0, EXCEL_CELL_MAX);
        }
        row.createCell(column).setCellValue(text);
    }

    private static void setNumericCell(Row row, int column, Number value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    private static String blankToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void addPagination(Model model, int rawTotalPages, int pageIndex) {
        int totalPages = Math.max(rawTotalPages, 1);
        int currentPage = pageIndex + 1;
        int windowStart = Math.max(1, currentPage - 2);
        int windowEnd = Math.min(totalPages, currentPage + 2);
        if (windowStart > windowEnd) {
            windowStart = windowEnd;
        }
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("windowStart", windowStart);
        model.addAttribute("windowEnd", windowEnd);
    }

    private record Filters(
            Long customerId,
            boolean customerRejected,
            String customerRaw,
            String plateRaw,
            String vinRaw
    ) {
        private Specification<IddaPlateCheck> spec() {
            return IddaPlateCheckSpec.filters(customerId, customerRejected, plateRaw, vinRaw);
        }

        private boolean active() {
            return customerRaw != null || plateRaw != null || vinRaw != null;
        }
    }
}
