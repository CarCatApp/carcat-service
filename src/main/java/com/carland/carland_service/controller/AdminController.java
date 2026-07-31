package com.carland.carland_service.controller;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.repository.CarRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final CarRepository carRepository;

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private static final int PAGE_SIZE = 10;

    private static final DateTimeFormatter EXCEL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    @GetMapping({"/admin", "/admin/"})
    public String loginPage(
            @RequestParam(required = false) String error,
            Model model
    ) {

        if (error != null) {
            model.addAttribute("error", true);
        }

        return "login";
    }


    @PostMapping("/admin/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session
    ) {

        if ("nemet".equals(username) && "nemet".equals(password)) {

            session.setAttribute("ADMIN_LOGIN", true);

            return "redirect:" + ADMIN_URL + "/admin/cars";
        }

        return "redirect:" + ADMIN_URL + "/admin/?error=true";
    }


    @GetMapping("/admin/cars")
    public String cars(
            @RequestParam(defaultValue = "1") int page,
            HttpSession session,
            Model model
    ) {

        if (!isLoggedIn(session)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }

        // URL 1'den başlar, Spring Data 0'dan — burada çeviriyoruz
        int pageIndex = Math.max(page, 1) - 1;

        Pageable pageable = PageRequest.of(pageIndex, PAGE_SIZE);

        Page<Car> carPage = carRepository.findAll(pageable);

        int totalPages = Math.max(carPage.getTotalPages(), 1);
        int currentPage = pageIndex + 1;

        // Numaralı sayfalama penceresi: aktif sayfanın ±2 komşusu
        int windowStart = Math.max(1, currentPage - 2);
        int windowEnd = Math.min(totalPages, currentPage + 2);

        // page > totalPages girilirse pencere ters dönmesin
        if (windowStart > windowEnd) {
            windowStart = windowEnd;
        }

        model.addAttribute("cars", carPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("windowStart", windowStart);
        model.addAttribute("windowEnd", windowEnd);
        model.addAttribute("pageSize", PAGE_SIZE);

        return "cars";
    }


    @GetMapping("/admin/cars/export")
    public void exportCars(
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {

        if (!isLoggedIn(session)) {
            response.sendRedirect(ADMIN_URL + "/admin/");
            return;
        }

        List<Car> cars = carRepository.findAll(Sort.by("carId"));

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Cars");

            String[] headers = {
                    "VIN", "Plate Number", "Brand", "Model", "Model Year",
                    "Engine Type", "Engine Type ID", "Engine Volume",
                    "Transmission Type", "Body Type", "Mileage",
                    "Created At", "Updated At"
            };

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Car car : cars) {

                Row row = sheet.createRow(rowIndex++);

                setCell(row, 0, car.getVin());
                setCell(row, 1, car.getPlateNumber());
                setCell(row, 2, car.getBrand());
                setCell(row, 3, car.getModel());
                setNumericCell(row, 4, car.getModelYear());
                setCell(row, 5, car.getEngineType());
                setNumericCell(row, 6, car.getEngineTypeId());
                setNumericCell(row, 7, car.getEngineVolume());
                setCell(row, 8, car.getTransmissionType());
                setCell(row, 9, car.getBodyType());
                setNumericCell(row, 10, car.getMileage());
                setCell(row, 11, formatDate(car.getCreatedAt()));
                setCell(row, 12, formatDate(car.getUpdatedAt()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            String fileName = "cars-" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            workbook.write(response.getOutputStream());
        }
    }


    @GetMapping("/admin/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:" + ADMIN_URL + "/admin/";
    }


    private boolean isLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("ADMIN_LOGIN"));
    }

    private void setCell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value != null ? value : "");
    }

    private void setNumericCell(Row row, int column, Number value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? EXCEL_DATE_FORMAT.format(dateTime) : "";
    }
}
