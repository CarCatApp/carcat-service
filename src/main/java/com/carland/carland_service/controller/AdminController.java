package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.AuthUser;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.feign.AuthUsersFeign;
import com.carland.carland_service.repository.CarRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final CarRepository carRepository;

    private final AuthUsersFeign authUsersFeign;

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


    // ==================== CARS ====================

    @GetMapping("/admin/cars")
    public String cars(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Long userId,
            HttpSession session,
            Model model
    ) {

        if (!isLoggedIn(session)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }

        // URL 1'den başlar, Spring Data 0'dan — burada çeviriyoruz
        int pageIndex = Math.max(page, 1) - 1;

        Pageable pageable = PageRequest.of(pageIndex, PAGE_SIZE);

        Page<Car> carPage = (userId != null)
                ? carRepository.findByCustomer_UserId(userId, pageable)
                : carRepository.findAll(pageable);

        addPaginationAttributes(model, carPage.getTotalPages(), pageIndex);

        model.addAttribute("cars", carPage);
        model.addAttribute("filterUserId", userId);

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

            createHeaderRow(workbook, sheet, headers);

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

            writeWorkbook(workbook, sheet, headers.length, "cars", response);
        }
    }


    // ==================== USERS (carland_auth) ====================

    @GetMapping("/admin/users")
    public String users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session,
            Model model
    ) {

        if (!isLoggedIn(session)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }

        List<AuthUser> allUsers;
        boolean loadError = false;

        try {
            allUsers = fetchUsers(from, to);
        } catch (Exception e) {
            log.error("carland_auth user listesi alınamadı", e);
            allUsers = Collections.emptyList();
            loadError = true;
        }

        // Liste uzak servisten geldiği için sayfalama burada, bellek üzerinde yapılır
        int pageIndex = Math.max(page, 1) - 1;
        int totalPages = Math.max((int) Math.ceil((double) allUsers.size() / PAGE_SIZE), 1);

        if (pageIndex >= totalPages) {
            pageIndex = totalPages - 1;
        }

        int fromIndex = pageIndex * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allUsers.size());

        List<AuthUser> pageContent = fromIndex < allUsers.size()
                ? allUsers.subList(fromIndex, toIndex)
                : Collections.emptyList();

        addPaginationAttributes(model, totalPages, pageIndex);

        model.addAttribute("users", pageContent);
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("loadError", loadError);

        return "users";
    }


    @GetMapping("/admin/users/export")
    public void exportUsers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {

        if (!isLoggedIn(session)) {
            response.sendRedirect(ADMIN_URL + "/admin/");
            return;
        }

        List<AuthUser> users = fetchUsers(from, to);

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Users");

            String[] headers = {"ID", "Name", "Surname", "Phone Number", "Status", "Created At"};

            createHeaderRow(workbook, sheet, headers);

            int rowIndex = 1;
            for (AuthUser user : users) {

                Row row = sheet.createRow(rowIndex++);

                setNumericCell(row, 0, user.getId());
                setCell(row, 1, user.getName());
                setCell(row, 2, user.getSurname());
                setCell(row, 3, user.getPhoneNumber());
                setCell(row, 4, user.getStatus());
                setCell(row, 5, formatDate(user.getCreatedAt()));
            }

            String baseName = (from != null || to != null) ? "users-filtered" : "users";

            writeWorkbook(workbook, sheet, headers.length, baseName, response);
        }
    }


    @GetMapping("/admin/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:" + ADMIN_URL + "/admin/";
    }


    // ==================== helpers ====================

    private boolean isLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("ADMIN_LOGIN"));
    }

    private List<AuthUser> fetchUsers(LocalDate from, LocalDate to) {
        return authUsersFeign.getUserList(
                from != null ? from.toString() : null,
                to != null ? to.toString() : null
        );
    }

    /** currentPage / totalPages / windowStart / windowEnd model attribute'larını doldurur (1 tabanlı). */
    private void addPaginationAttributes(Model model, int rawTotalPages, int pageIndex) {

        int totalPages = Math.max(rawTotalPages, 1);
        int currentPage = pageIndex + 1;

        // Numaralı sayfalama penceresi: aktif sayfanın ±2 komşusu
        int windowStart = Math.max(1, currentPage - 2);
        int windowEnd = Math.min(totalPages, currentPage + 2);

        // page > totalPages girilirse pencere ters dönmesin
        if (windowStart > windowEnd) {
            windowStart = windowEnd;
        }

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("windowStart", windowStart);
        model.addAttribute("windowEnd", windowEnd);
    }

    private void createHeaderRow(Workbook workbook, Sheet sheet, String[] headers) {

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
    }

    private void writeWorkbook(
            Workbook workbook,
            Sheet sheet,
            int columnCount,
            String baseName,
            HttpServletResponse response
    ) throws IOException {

        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }

        String fileName = baseName + "-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        workbook.write(response.getOutputStream());
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
