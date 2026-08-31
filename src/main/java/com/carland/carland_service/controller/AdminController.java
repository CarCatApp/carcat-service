package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.AdminAuthLoginResponse;
import com.carland.carland_service.dto.response.AuthUser;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Feedback;
import com.carland.carland_service.entity.FeedbackPhoto;
import com.carland.carland_service.feign.AuthNewUsersFeign;
import com.carland.carland_service.feign.AuthUsersFeign;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.FeedbackPhotoRepository;
import com.carland.carland_service.repository.FeedbackRepository;
import com.carland.carland_service.security.AdminAccessService;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * tr: Admin panelinin MVC controller'ı; login/logout, araç ve kullanıcı listelerini sayfalayarak gösterir ve Excel (XLSX) export sağlar.
 * en: MVC controller for the admin panel; handles login/logout, shows paginated car and user lists, and provides Excel (XLSX) exports.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final CarRepository carRepository;

    private final FeedbackRepository feedbackRepository;

    private final FeedbackPhotoRepository feedbackPhotoRepository;

    private final AuthUsersFeign authUsersFeign;

    private final AuthNewUsersFeign authNewUsersFeign;

    private final AdminAccessService adminAccessService;

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private static final int PAGE_SIZE = 10;

    private static final DateTimeFormatter EXCEL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    /**
     * tr: Admin giriş sayfasını döner; "error" parametresi varsa modele hata bayrağı ekleyip login şablonunu render eder.
     * en: Returns the admin login page; if the "error" query parameter is present, adds an error flag to the model and renders the login template.
     */
    @GetMapping({"/admin", "/admin/"})
    public String loginPage(
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            Model model
    ) {
        if (adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/cars";
        }
        if (error != null) {
            model.addAttribute("error", true);
        }
        return "login";
    }


    /**
     * tr: Auth JWT login; yalnızca +994500000000 / ADMIN panele girer, diğer herkes "Admin not found".
     * en: Auth JWT login; only +994500000000 / ADMIN may enter the panel, everyone else gets "Admin not found".
     */
    @PostMapping("/admin/login")
    public String login(
            @RequestParam String phoneNumber,
            @RequestParam String pin,
            HttpServletResponse response
    ) {
        try {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("phoneNumber", phoneNumber);
            body.put("pinCode", pin);
            body.put("deviceId", "admin-panel");
            body.put("platform", "ADMIN_PANEL");
            AdminAuthLoginResponse auth = authNewUsersFeign.login(body, "az");
            if (auth == null
                    || auth.getAccessToken() == null
                    || auth.getAccessToken().isBlank()
                    || !adminAccessService.getPanelPhone().equals(auth.getPhoneNumber())
                    || !"ADMIN".equalsIgnoreCase(auth.getRole())) {
                return "redirect:" + ADMIN_URL + "/admin/?error=not_found";
            }
            adminAccessService.writeCookie(response, auth.getAccessToken());
            return "redirect:" + ADMIN_URL + "/admin/cars";
        } catch (Exception ex) {
            log.warn("Admin panel login rejected: {}", ex.getMessage());
            return "redirect:" + ADMIN_URL + "/admin/?error=not_found";
        }
    }


    // ==================== CARS ====================

    /**
     * tr: Araç listesini sayfa sayfa gösterir; opsiyonel userId filtresi uygular, login yoksa admin giriş sayfasına yönlendirir.
     * en: Shows the car list page by page; applies an optional userId filter and redirects to the admin login page if the session is not authenticated.
     */
    @GetMapping("/admin/cars")
    public String cars(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request,
            Model model
    ) {

        if (!adminAccessService.isPanelAdmin(request)) {
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


    /**
     * tr: Tüm araçları XLSX dosyası olarak indirir (VIN, plaka, marka, model, km vb. kolonlarla); login yoksa admin giriş sayfasına yönlendirir.
     * en: Downloads all cars as an XLSX file (columns for VIN, plate, brand, model, mileage etc.); redirects to the admin login page if not authenticated.
     */
    @GetMapping("/admin/cars/export")
    public void exportCars(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        if (!adminAccessService.isPanelAdmin(request)) {
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

    /**
     * tr: carland_auth servisinden kullanıcı listesini çekip (opsiyonel from/to tarih filtresiyle) bellekte sayfalayarak gösterir; uzak servis hatasında boş liste ve loadError bayrağı döner, login yoksa giriş sayfasına yönlendirir.
     * en: Fetches the user list from the carland_auth service (with optional from/to date filters) and paginates it in memory; on remote-service failure shows an empty list with a loadError flag, and redirects to login if not authenticated.
     */
    @GetMapping("/admin/users")
    public String users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request,
            Model model
    ) {

        if (!adminAccessService.isPanelAdmin(request)) {
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


    /**
     * tr: carland_auth kullanıcılarını (opsiyonel from/to tarih filtresiyle) XLSX dosyası olarak indirir; login yoksa admin giriş sayfasına yönlendirir.
     * en: Downloads carland_auth users as an XLSX file (with optional from/to date filters); redirects to the admin login page if not authenticated.
     */
    @GetMapping("/admin/users/export")
    public void exportUsers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        if (!adminAccessService.isPanelAdmin(request)) {
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


    // ==================== FEEDBACKS ====================

    /**
     * tr: Uygulama geri bildirimlerini (feedback / support / bug_report) sayfalayarak gösterir.
     * en: Lists in-app feedbacks (feedback / support / bug_report) with pagination.
     */
    @GetMapping("/admin/feedbacks")
    public String feedbacks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String withPhoto,
            HttpServletRequest request,
            Model model
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }

        boolean photoOnly = isPhotoOnly(withPhoto);
        int pageIndex = Math.max(page, 1) - 1;
        Pageable pageable = PageRequest.of(pageIndex, PAGE_SIZE);
        Page<Feedback> feedbackPage = fetchFeedbacks(type, phone, photoOnly, pageable);

        addPaginationAttributes(model, feedbackPage.getTotalPages(), pageIndex);
        model.addAttribute("feedbacks", feedbackPage);
        model.addAttribute("filterType", blankToNull(type));
        model.addAttribute("filterPhone", blankToNull(phone));
        model.addAttribute("filterWithPhoto", photoOnly);
        model.addAttribute("photoIds", photoIdsOf(feedbackPage.getContent()));
        return "feedbacks";
    }

    /**
     * tr: Geri bildirimleri (aynı filtrelerle) XLSX olarak indirir.
     * en: Downloads feedbacks as XLSX using the same filters.
     */
    @GetMapping("/admin/feedbacks/export")
    public void exportFeedbacks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String withPhoto,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (!adminAccessService.isPanelAdmin(request)) {
            response.sendRedirect(ADMIN_URL + "/admin/");
            return;
        }

        boolean photoOnly = isPhotoOnly(withPhoto);
        List<Feedback> rows = fetchFeedbacks(type, phone, photoOnly, Pageable.unpaged()).getContent();
        Set<Long> photoIds = photoIdsOf(rows);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Feedbacks");
            String[] headers = {
                    "ID", "Type", "Subject", "Description", "Rating", "Customer ID", "Phone", "Has photo"
            };
            createHeaderRow(workbook, sheet, headers);
            int rowIndex = 1;
            for (Feedback fb : rows) {
                Row row = sheet.createRow(rowIndex++);
                setNumericCell(row, 0, fb.getFeedbackId());
                setCell(row, 1, fb.getType());
                setCell(row, 2, fb.getSubject());
                setCell(row, 3, fb.getDescription());
                setNumericCell(row, 4, fb.getRating());
                setNumericCell(row, 5, fb.getCustomerId());
                setCell(row, 6, fb.getCustomerPhone());
                setCell(row, 7, photoIds.contains(fb.getFeedbackId()) ? "Yes" : "No");
            }
            String baseName = (blankToNull(type) != null || blankToNull(phone) != null || photoOnly)
                    ? "feedbacks-filtered" : "feedbacks";
            writeWorkbook(workbook, sheet, headers.length, baseName, response);
        }
    }

    /**
     * tr: Panel admin için feedback resmini byte olarak döner (lightbox preview).
     * en: Returns the feedback image bytes for the panel admin (lightbox preview).
     */
    @GetMapping("/admin/feedbacks/{id}/photo")
    public ResponseEntity<byte[]> feedbackPhoto(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        FeedbackPhoto photo = feedbackPhotoRepository.findByFeedbackId(id).orElse(null);
        if (photo == null || photo.getImageData() == null || photo.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        String fileType = photo.getFileType();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileType != null && !fileType.isBlank()) {
            String raw = fileType.contains("/") ? fileType : "image/" + fileType.toLowerCase();
            try {
                mediaType = MediaType.parseMediaType(raw);
            } catch (Exception ignored) {
                mediaType = MediaType.IMAGE_JPEG;
            }
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(photo.getImageData());
    }

    private Page<Feedback> fetchFeedbacks(String type, String phone, boolean withPhoto, Pageable pageable) {
        String typeFilter = blankToNull(type);
        if (typeFilter != null) {
            typeFilter = typeFilter.toLowerCase();
        }
        String phoneFilter = blankToNull(phone);
        if (phoneFilter != null) {
            phoneFilter = "%" + phoneFilter.toLowerCase() + "%";
        }
        return feedbackRepository.search(typeFilter, phoneFilter, withPhoto, pageable);
    }

    private Set<Long> photoIdsOf(List<Feedback> rows) {
        if (rows == null || rows.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = rows.stream().map(Feedback::getFeedbackId).collect(Collectors.toList());
        return new HashSet<>(feedbackPhotoRepository.findFeedbackIdsWithPhoto(ids));
    }

    private static boolean isPhotoOnly(String withPhoto) {
        if (withPhoto == null || withPhoto.isBlank()) {
            return false;
        }
        String v = withPhoto.trim();
        return "1".equals(v) || "true".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v);
    }

    private static String blankToNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }


    /**
     * tr: Admin oturumunu (session) sonlandırır ve giriş sayfasına yönlendirir.
     * en: Invalidates the admin session and redirects to the login page.
     */
    @GetMapping("/admin/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        adminAccessService.clearCookie(response);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:" + ADMIN_URL + "/admin/";
    }


    // ==================== helpers ====================

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

    /**
     * tr: DB Frankfurt (UTC) zamanını Azerbaycan (UTC+4) için +4 saat kaydırarak formatlar.
     * en: Formats DB Frankfurt (UTC) time shifted +4 hours for Azerbaijan (UTC+4) display.
     */
    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? EXCEL_DATE_FORMAT.format(dateTime.plusHours(4)) : "";
    }
}
