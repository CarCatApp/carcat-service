package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.AdminAuthLoginResponse;
import com.carland.carland_service.dto.response.AdminSimaAttemptRow;
import com.carland.carland_service.dto.response.AdminSimaPanel;
import com.carland.carland_service.dto.response.AuthUser;
import com.carland.carland_service.dto.response.PartnerDataResponse;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Feedback;
import com.carland.carland_service.entity.SimaKycRecord;
import com.carland.carland_service.entity.FeedbackPhoto;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.feign.AuthNewUsersFeign;
import com.carland.carland_service.feign.AuthUsersFeign;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CarSpec;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.FeedbackPhotoRepository;
import com.carland.carland_service.repository.FeedbackRepository;
import com.carland.carland_service.repository.FeedbackSpec;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.repository.SimaKycRecordRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.test_sima_idda.service.SimaAttemptLimitService;
import com.carland.carland_service.service.AdminCarPurgeService;
import com.carland.carland_service.service.PhotoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    private final CustomerRepository customerRepository;

    private final VisitRepository visitRepository;

    private final PartnerRepository partnerRepository;

    private final FeedbackRepository feedbackRepository;

    private final FeedbackPhotoRepository feedbackPhotoRepository;

    private final AuthUsersFeign authUsersFeign;

    private final AuthNewUsersFeign authNewUsersFeign;

    private final AdminAccessService adminAccessService;

    private final PhotoService photoService;

    private final SimaAttemptLimitService simaAttemptLimitService;

    private final SimaKycRecordRepository simaKycRecordRepository;

    private final AdminCarPurgeService adminCarPurgeService;

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
     * tr: Auth JWT login; yalnızca panel telefonu girer.
     * en: Auth JWT login; only the panel phone may enter.
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
                    || !adminAccessService.getPanelPhone().equals(auth.getPhoneNumber())) {
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
     * tr: Araç listesini sayfa sayfa gösterir; VIN / userId filtresi, carId DESC (yeni kayıt üstte).
     * en: Paginated car list; VIN / userId filters, carId DESC (newest first).
     */
    @GetMapping("/admin/cars")
    public String cars(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String vin,
            HttpServletRequest request,
            Model model
    ) {

        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }

        int pageIndex = Math.max(page, 1) - 1;
        String vinFilter = blankToNull(vin);
        if (vinFilter != null) {
            vinFilter = vinFilter.replace(" ", "");
        }
        Pageable pageable = PageRequest.of(pageIndex, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "carId"));
        Page<Car> carPage = carRepository.findAll(CarSpec.filters(userId, vinFilter), pageable);

        addPaginationAttributes(model, carPage.getTotalPages(), pageIndex);

        model.addAttribute("cars", carPage);
        model.addAttribute("filterUserId", userId);
        model.addAttribute("filterVin", vinFilter);

        return "cars";
    }


    /**
     * tr: Tüm araçları XLSX dosyası olarak indirir (VIN, plaka, marka, model, km vb. kolonlarla); login yoksa admin giriş sayfasına yönlendirir.
     * en: Downloads all cars as an XLSX file (columns for VIN, plate, brand, model, mileage etc.); redirects to the admin login page if not authenticated.
     */
    @GetMapping("/admin/cars/export")
    public void exportCars(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String vin,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        if (!adminAccessService.isPanelAdmin(request)) {
            response.sendRedirect(ADMIN_URL + "/admin/");
            return;
        }

        String vinFilter = blankToNull(vin);
        if (vinFilter != null) {
            vinFilter = vinFilter.replace(" ", "");
        }
        List<Car> cars = carRepository.findAll(
                CarSpec.filters(userId, vinFilter),
                Sort.by(Sort.Direction.DESC, "carId"));

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

            String baseName = (userId != null || vinFilter != null) ? "cars-filtered" : "cars";
            writeWorkbook(workbook, sheet, headers.length, baseName, response);
        }
    }


    /**
     * tr: Araç partner servis geçmişini (visits) DB'den gösterir; Hyper API çağırmaz.
     * en: Shows the car's partner service history from DB visits; does not call Hyper.
     */
    @Transactional(readOnly = true)
    @GetMapping("/admin/cars/{carId:\\d+}/history")
    public String carHistory(
            @PathVariable Long carId,
            HttpServletRequest request,
            Model model
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }

        Car car = carRepository.findByCarId(carId);
        if (car == null) {
            model.addAttribute("carMissing", true);
            model.addAttribute("visitCountByPartnerId", Map.of());
            return "car-history";
        }

        List<Visit> visits = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car);
        for (Visit visit : visits) {
            if (visit.getParts() != null) {
                visit.getParts().size();
            }
        }

        Map<Long, PartnerDataResponse> partnerById = loadAdminPartners(visits);
        Map<Long, Long> visitCountByPartnerId = visits.stream()
                .map(Visit::getServiceCenterId)
                .filter(id -> id != null)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        List<PartnerDataResponse> partnerChips = new ArrayList<>(partnerById.values());
        partnerChips.sort(Comparator.comparing(PartnerDataResponse::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        String phone = car.getCustomer() != null ? car.getCustomer().getPhoneNumber() : null;

        model.addAttribute("carMissing", false);
        model.addAttribute("car", car);
        model.addAttribute("visits", visits);
        model.addAttribute("partnerById", partnerById);
        model.addAttribute("partnerChips", partnerChips);
        model.addAttribute("visitCountByPartnerId", visitCountByPartnerId);
        model.addAttribute("customerPhone", phone);
        return "car-history";
    }

    /**
     * tr: History ekranından aracı ve bağlı visit/percentage/foto/history satırlarını siler; VIN onayı şart.
     * en: From the history screen, deletes the car and related visit/percentage/photo/history rows; VIN confirmation required.
     */
    @PostMapping("/admin/cars/{carId:\\d+}/delete")
    public String deleteCar(
            @PathVariable Long carId,
            @RequestParam(required = false) String confirmVin,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        AdminCarPurgeService.DeleteOutcome outcome = adminCarPurgeService.deleteCompletely(carId, confirmVin);
        if (outcome == AdminCarPurgeService.DeleteOutcome.DELETED) {
            redirectAttributes.addFlashAttribute("carsMessage", "Araç silindi (carId=" + carId + ").");
            return "redirect:" + ADMIN_URL + "/admin/cars";
        }
        if (outcome == AdminCarPurgeService.DeleteOutcome.VIN_MISMATCH) {
            redirectAttributes.addFlashAttribute("historyError", "VIN eşleşmedi. Delete car iptal.");
            return "redirect:" + ADMIN_URL + "/admin/cars/" + carId + "/history";
        }
        redirectAttributes.addFlashAttribute("carsMessage", "Araç bulunamadı.");
        return "redirect:" + ADMIN_URL + "/admin/cars";
    }

    /**
     * tr: History ekranından aracı müşteri listesinden çıkarır (customer_id null) ve carlist cache'ini düşürür.
     * en: From the history screen, unlinks the car from the customer (customer_id null) and evicts carlist cache.
     */
    @PostMapping("/admin/cars/{carId:\\d+}/unlink-customer")
    public String unlinkCarFromCustomer(
            @PathVariable Long carId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        AdminCarPurgeService.UnlinkOutcome outcome = adminCarPurgeService.unlinkFromCustomer(carId);
        if (outcome == AdminCarPurgeService.UnlinkOutcome.UNLINKED) {
            redirectAttributes.addFlashAttribute("historyMessage", "Araç müşteri listesinden çıkarıldı. Satır ve history duruyor.");
        } else if (outcome == AdminCarPurgeService.UnlinkOutcome.ALREADY_ORPHAN) {
            redirectAttributes.addFlashAttribute("historyMessage", "Bu araç zaten bir müşteriye bağlı değil.");
        } else {
            redirectAttributes.addFlashAttribute("historyError", "Araç bulunamadı.");
            return "redirect:" + ADMIN_URL + "/admin/cars";
        }
        return "redirect:" + ADMIN_URL + "/admin/cars/" + carId + "/history";
    }

    /**
     * tr: Admin history için partners satırlarını (secret'sız) yükler; visit'te görülen ama tabloda olmayan id'ler de eklenir.
     * en: Loads partners for admin history without secrets; also adds ids seen on visits but missing from the table.
     */
    private Map<Long, PartnerDataResponse> loadAdminPartners(List<Visit> visits) {
        List<Partner> rows = partnerRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        Map<Long, PartnerDataResponse> byId = new LinkedHashMap<>();
        for (Partner partner : rows) {
            byId.put(partner.getId(), PartnerDataResponse.builder()
                    .id(partner.getId())
                    .name(partner.getName())
                    .logoUrl(partner.getLogoUrl())
                    .active(partner.getActive())
                    .source(partner.getSource())
                    .build());
        }
        for (Visit visit : visits) {
            Long id = visit.getServiceCenterId();
            if (id == null || byId.containsKey(id)) {
                continue;
            }
            String fallbackName = visit.getServiceCenterName();
            byId.put(id, PartnerDataResponse.builder()
                    .id(id)
                    .name(fallbackName != null && !fallbackName.isBlank() ? fallbackName : ("id " + id))
                    .dealer(visit.getDealer())
                    .active(false)
                    .build());
        }
        return byId;
    }


    /**
     * tr: Admin history partner listesi (secret yok). GET /api/v1/partner list yok; panel cookie ile buradan okur.
     * en: Admin history partner list (no secrets). There is no public GET /api/v1/partner list; panel reads here with cookie.
     */
    @GetMapping(value = "/admin/partners", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<PartnerDataResponse>> adminPartners(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<PartnerDataResponse> rows = new ArrayList<>(loadAdminPartners(List.of()).values());
        rows.sort(Comparator.comparing(PartnerDataResponse::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return ResponseEntity.ok(rows);
    }

    /**
     * tr: Partner logosunu döner — aynı veri {@code GET /api/v1/photo/for/partner/get/{partnerId}}, panel cookie auth.
     * en: Returns the partner logo — same data as {@code GET /api/v1/photo/for/partner/get/{partnerId}}, cookie auth for the panel.
     */
    @GetMapping("/admin/partners/{id:\\d+}/photo")
    public ResponseEntity<byte[]> adminPartnerPhoto(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return photoService.getPartnerPhotoById(id);
        } catch (ResourceNotFoundException | MissingFieldException ex) {
            return ResponseEntity.notFound().build();
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
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "all") String sima,
            @RequestParam(required = false) Long attempts,
            HttpServletRequest request,
            Model model
    ) {

        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }

        String phoneFilter = blankToNull(phone);
        String simaFilter = normalizeSimaFilter(sima);
        List<AuthUser> allUsers;
        boolean loadError = false;

        try {
            allUsers = fetchUsers(from, to, phoneFilter);
        } catch (Exception e) {
            log.error("carland_auth user listesi alınamadı", e);
            allUsers = Collections.emptyList();
            loadError = true;
        }

        Map<Long, Customer> customersById = new LinkedHashMap<>();
        Map<String, Customer> customersByPhone = new LinkedHashMap<>();
        indexCustomers(customersById, customersByPhone);

        if (!"all".equals(simaFilter)) {
            boolean wantVerified = "yes".equals(simaFilter);
            allUsers = allUsers.stream()
                    .filter(user -> isSimaVerified(resolveCustomer(user, customersById, customersByPhone)) == wantVerified)
                    .collect(Collectors.toList());
        }

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
        applyAdminDisplayNames(pageContent, customersById, customersByPhone);

        Map<Long, Boolean> simaByUserId = new LinkedHashMap<>();
        for (AuthUser user : pageContent) {
            if (user.getId() == null) {
                continue;
            }
            simaByUserId.put(user.getId(), isSimaVerified(resolveCustomer(user, customersById, customersByPhone)));
        }

        addPaginationAttributes(model, totalPages, pageIndex);

        model.addAttribute("users", pageContent);
        model.addAttribute("simaByUserId", simaByUserId);
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("filterPhone", phoneFilter);
        model.addAttribute("filterSima", simaFilter);
        model.addAttribute("attemptsUserId", attempts);
        model.addAttribute("loadError", loadError);
        model.addAttribute("simaPanel", attempts == null ? null : buildSimaPanel(attempts, allUsers, customersById, customersByPhone));

        return "users";
    }

    @PostMapping("/admin/users/sima/reset")
    public String resetSimaLimit(
            @RequestParam Long customerUserId,
            @RequestParam String kind,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "all") String sima,
            @RequestParam(required = false) Long attempts,
            HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        Customer customer = customerRepository.findByUserId(customerUserId);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("simaMessage", "Müştəri tapılmadı.");
        } else if ("daily".equalsIgnoreCase(kind)) {
            simaAttemptLimitService.resetDaily(customer);
            customerRepository.save(customer);
            redirectAttributes.addFlashAttribute("simaMessage", "Günlük limit sıfırlandı. Köhnə cəhdlər silinmedi.");
        } else if ("ever".equalsIgnoreCase(kind)) {
            simaAttemptLimitService.resetEver(customer);
            customerRepository.save(customer);
            redirectAttributes.addFlashAttribute("simaMessage", "Ümumi limit sıfırlandı. Köhnə cəhdlər silinmedi.");
        } else {
            redirectAttributes.addFlashAttribute("simaMessage", "Naməlum limit növü.");
        }
        return usersRedirect(page, from, to, phone, sima, attempts);
    }

    @PostMapping("/admin/users/sima/clear-verified")
    public String clearSimaVerified(
            @RequestParam Long customerUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "all") String sima,
            @RequestParam(required = false) Long attempts,
            HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        Customer customer = customerRepository.findByUserId(customerUserId);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("simaMessage", "Müştəri tapılmadı.");
        } else if (!Boolean.TRUE.equals(customer.getSimaVerified())) {
            redirectAttributes.addFlashAttribute("simaMessage", "Bu müştəri artıq Sima təsdiqli deyil.");
        } else {
            customer.setSimaVerified(false);
            customerRepository.save(customer);
            redirectAttributes.addFlashAttribute("simaMessage", "Sima təsdiq statusu silindi. Ad, FIN və cəhdlər saxlanıldı.");
        }
        return usersRedirect(page, from, to, phone, sima, attempts);
    }


    /**
     * tr: carland_auth kullanıcılarını (opsiyonel from/to tarih filtresiyle) XLSX dosyası olarak indirir; login yoksa admin giriş sayfasına yönlendirir.
     * en: Downloads carland_auth users as an XLSX file (with optional from/to date filters); redirects to the admin login page if not authenticated.
     */
    @GetMapping("/admin/users/export")
    public void exportUsers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "all") String sima,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        if (!adminAccessService.isPanelAdmin(request)) {
            response.sendRedirect(ADMIN_URL + "/admin/");
            return;
        }

        String phoneFilter = blankToNull(phone);
        String simaFilter = normalizeSimaFilter(sima);
        List<AuthUser> users = fetchUsers(from, to, phoneFilter);
        Map<Long, Customer> customersById = new LinkedHashMap<>();
        Map<String, Customer> customersByPhone = new LinkedHashMap<>();
        indexCustomers(customersById, customersByPhone);
        if (!"all".equals(simaFilter)) {
            boolean wantVerified = "yes".equals(simaFilter);
            users = users.stream()
                    .filter(user -> isSimaVerified(resolveCustomer(user, customersById, customersByPhone)) == wantVerified)
                    .collect(Collectors.toList());
        }

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Users");

            String[] headers = {"ID", "Name", "Surname", "Phone Number", "Status", "SIMA", "Created At"};

            createHeaderRow(workbook, sheet, headers);

            int rowIndex = 1;
            applyAdminDisplayNames(users, customersById, customersByPhone);
            for (AuthUser user : users) {

                Row row = sheet.createRow(rowIndex++);

                setNumericCell(row, 0, user.getId());
                setCell(row, 1, user.getName());
                setCell(row, 2, user.getSurname());
                setCell(row, 3, user.getPhoneNumber());
                setCell(row, 4, user.getStatus());
                setCell(row, 5, isSimaVerified(resolveCustomer(user, customersById, customersByPhone)) ? "Yes" : "No");
                setCell(row, 6, formatDate(user.getCreatedAt()));
            }

            String baseName = (from != null || to != null || phoneFilter != null || !"all".equals(simaFilter))
                    ? "users-filtered" : "users";

            writeWorkbook(workbook, sheet, headers.length, baseName, response);
        }
    }

    /**
     * tr: sima_kyc_records tablosunun tamamını XLSX olarak indirir (admin cookie).
     * en: Downloads the full sima_kyc_records table as XLSX (admin cookie).
     */
    @GetMapping("/admin/users/sima-transactions/export")
    @Transactional(readOnly = true)
    public void exportSimaTransactions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!adminAccessService.isPanelAdmin(request)) {
            response.sendRedirect(ADMIN_URL + "/admin/");
            return;
        }
        List<SimaKycRecord> rows = simaKycRecordRepository.findAllWithCustomerOrderByIdAsc();
        String[] headers = {
                "id", "customer_id", "channel", "verified", "applied_to_profile", "idempotency_key",
                "transaction_id", "process_time", "pin", "document_number", "name", "surname", "patronymic",
                "birth_date", "birth_address", "address", "nationality", "gender", "exp_date",
                "document_type", "issuing_country", "liveness_score", "liveness_status", "liveness_failure_reason",
                "similarity_score", "similarity_status", "sima_http_status", "sima_response_code", "sima_message",
                "outcome", "created_at"
        };
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("sima_kyc_records");
            createHeaderRow(workbook, sheet, headers);
            int rowIndex = 1;
            for (SimaKycRecord rec : rows) {
                Row row = sheet.createRow(rowIndex++);
                Long customerId = rec.getCustomer() == null ? null : rec.getCustomer().getUserId();
                setNumericCell(row, 0, rec.getId());
                setNumericCell(row, 1, customerId);
                setCell(row, 2, rec.getChannel());
                setCell(row, 3, String.valueOf(rec.isVerified()));
                setCell(row, 4, String.valueOf(rec.isAppliedToProfile()));
                setCell(row, 5, rec.getIdempotencyKey());
                setCell(row, 6, rec.getTransactionId());
                setCell(row, 7, rec.getProcessTime());
                setCell(row, 8, rec.getPin());
                setCell(row, 9, rec.getDocumentNumber());
                setCell(row, 10, rec.getName());
                setCell(row, 11, rec.getSurname());
                setCell(row, 12, rec.getPatronymic());
                setCell(row, 13, rec.getBirthDate());
                setCell(row, 14, rec.getBirthAddress());
                setCell(row, 15, rec.getAddress());
                setCell(row, 16, rec.getNationality());
                setCell(row, 17, rec.getGender());
                setCell(row, 18, rec.getExpDate());
                setCell(row, 19, rec.getDocumentType());
                setCell(row, 20, rec.getIssuingCountry());
                setNumericCell(row, 21, rec.getLivenessScore());
                setCell(row, 22, boolCell(rec.getLivenessStatus()));
                setCell(row, 23, rec.getLivenessFailureReason());
                setNumericCell(row, 24, rec.getSimilarityScore());
                setCell(row, 25, boolCell(rec.getSimilarityStatus()));
                setNumericCell(row, 26, rec.getSimaHttpStatus());
                setNumericCell(row, 27, rec.getSimaResponseCode());
                setCell(row, 28, rec.getSimaMessage());
                setCell(row, 29, rec.getOutcome());
                setCell(row, 30, formatDate(rec.getCreatedAt()));
            }
            writeWorkbook(workbook, sheet, headers.length, "sima-transactions", response);
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
            @RequestParam(required = false) String sort,
            HttpServletRequest request,
            Model model
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }

        boolean photoOnly = isPhotoOnly(withPhoto);
        boolean oldestFirst = isOldestFirst(sort);
        int pageIndex = Math.max(page, 1) - 1;
        Pageable pageable = PageRequest.of(pageIndex, PAGE_SIZE);
        Page<Feedback> feedbackPage = fetchFeedbacks(type, phone, photoOnly, oldestFirst, pageable);

        addPaginationAttributes(model, feedbackPage.getTotalPages(), pageIndex);
        model.addAttribute("feedbacks", feedbackPage);
        model.addAttribute("filterType", blankToNull(type));
        model.addAttribute("filterPhone", blankToNull(phone));
        model.addAttribute("filterWithPhoto", photoOnly);
        model.addAttribute("filterOldestFirst", oldestFirst);
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
            @RequestParam(required = false) String sort,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (!adminAccessService.isPanelAdmin(request)) {
            response.sendRedirect(ADMIN_URL + "/admin/");
            return;
        }

        boolean photoOnly = isPhotoOnly(withPhoto);
        boolean oldestFirst = isOldestFirst(sort);
        List<Feedback> rows = fetchFeedbacks(type, phone, photoOnly, oldestFirst, Pageable.unpaged()).getContent();
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
            String baseName = (blankToNull(type) != null || blankToNull(phone) != null || photoOnly || oldestFirst)
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

    private Page<Feedback> fetchFeedbacks(
            String type, String phone, boolean withPhoto, boolean oldestFirst, Pageable pageable
    ) {
        var spec = FeedbackSpec.filters(blankToNull(type), blankToNull(phone), withPhoto);
        Sort sort = Sort.by(oldestFirst ? Sort.Direction.ASC : Sort.Direction.DESC, "feedbackId");
        if (pageable.isUnpaged()) {
            return new PageImpl<>(feedbackRepository.findAll(spec, sort));
        }
        return feedbackRepository.findAll(
                spec,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));
    }

    private Set<Long> photoIdsOf(List<Feedback> rows) {
        if (rows == null || rows.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = rows.stream().map(Feedback::getFeedbackId).collect(Collectors.toList());
        return new HashSet<>(feedbackPhotoRepository.findFeedbackIdsWithPhoto(ids));
    }

    private static boolean isOldestFirst(String sort) {
        return sort != null && "asc".equalsIgnoreCase(sort.trim());
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

    private static String normalizeSimaFilter(String raw) {
        if (raw == null) {
            return "all";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "yes".equals(value) || "no".equals(value) ? value : "all";
    }

    private void indexCustomers(Map<Long, Customer> byId, Map<String, Customer> byPhone) {
        for (Customer customer : customerRepository.findAll()) {
            if (customer.getUserId() != null) {
                byId.put(customer.getUserId(), customer);
            }
            String phone = digits(customer.getPhoneNumber());
            if (phone != null) {
                byPhone.putIfAbsent(phone, customer);
            }
        }
    }

    private Customer resolveCustomer(AuthUser user, Map<Long, Customer> byId, Map<String, Customer> byPhone) {
        if (user.getId() != null && byId.containsKey(user.getId())) {
            return byId.get(user.getId());
        }
        String phone = digits(user.getPhoneNumber());
        return phone == null ? null : byPhone.get(phone);
    }

    private static boolean isSimaVerified(Customer customer) {
        return customer != null && Boolean.TRUE.equals(customer.getSimaVerified());
    }

    private void applyAdminDisplayNames(
            List<AuthUser> users,
            Map<Long, Customer> byId,
            Map<String, Customer> byPhone
    ) {
        for (AuthUser user : users) {
            applyAdminDisplayName(user, resolveCustomer(user, byId, byPhone));
        }
    }

    /**
     * tr: SIMA verified ise adı/soyadı kayıtsız customers'tan alır. Değilse dolu olan kaynağı kullanır (önce customer, boşsa auth).
     * en: If SIMA-verified, name/surname come from customers with no extra checks. Otherwise the non-blank source wins (customer first, else auth).
     */
    private static void applyAdminDisplayName(AuthUser user, Customer customer) {
        if (user == null) {
            return;
        }
        if (isSimaVerified(customer)) {
            user.setName(blankToNull(customer.getName()));
            user.setSurname(blankToNull(customer.getSurname()));
            return;
        }
        user.setName(firstNonBlank(customer == null ? null : customer.getName(), user.getName()));
        user.setSurname(firstNonBlank(customer == null ? null : customer.getSurname(), user.getSurname()));
    }

    private static String firstNonBlank(String primary, String fallback) {
        String value = blankToNull(primary);
        return value != null ? value : blankToNull(fallback);
    }

    private static String digits(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private AdminSimaPanel buildSimaPanel(
            Long authUserId,
            List<AuthUser> users,
            Map<Long, Customer> byId,
            Map<String, Customer> byPhone
    ) {
        AuthUser user = users.stream()
                .filter(row -> authUserId.equals(row.getId()))
                .findFirst()
                .orElse(null);
        if (user == null) {
            return null;
        }
        Customer customer = resolveCustomer(user, byId, byPhone);
        if (customer == null) {
            return AdminSimaPanel.builder()
                    .authUserId(authUserId)
                    .phone(user.getPhoneNumber())
                    .customerFound(false)
                    .simaVerified(false)
                    .dailyUsed(0)
                    .dailyLimit(simaAttemptLimitService.dailyLimit())
                    .totalUsed(0)
                    .totalLimit(simaAttemptLimitService.totalLimit())
                    .attempts(List.of())
                    .build();
        }
        List<AdminSimaAttemptRow> rows = simaAttemptLimitService.listAttempts(customer).stream()
                .map(record -> toAttemptRow(customer, record))
                .collect(Collectors.toList());
        return AdminSimaPanel.builder()
                .authUserId(authUserId)
                .customerUserId(customer.getUserId())
                .phone(user.getPhoneNumber())
                .customerFound(true)
                .simaVerified(isSimaVerified(customer))
                .dailyUsed((int) simaAttemptLimitService.countDailyFails(customer))
                .dailyLimit(simaAttemptLimitService.dailyLimit())
                .totalUsed((int) simaAttemptLimitService.countTotal(customer))
                .totalLimit(simaAttemptLimitService.totalLimit())
                .attempts(rows)
                .build();
    }

    private AdminSimaAttemptRow toAttemptRow(Customer customer, SimaKycRecord record) {
        boolean beforeDaily = isBeforeReset(record.getCreatedAt(), customer.getSimaDailyLimitResetAt());
        boolean beforeEver = isBeforeReset(record.getCreatedAt(), customer.getSimaEverLimitResetAt());
        String beforeLabel = "";
        if (beforeDaily && beforeEver) {
            beforeLabel = "günlük + ümumi limitdən əvvəl";
        } else if (beforeDaily) {
            beforeLabel = "günlük limitdən əvvəl";
        } else if (beforeEver) {
            beforeLabel = "ümumi limitdən əvvəl";
        }
        return AdminSimaAttemptRow.builder()
                .time(record.getCreatedAt() == null ? "—" : record.getCreatedAt().format(SIMA_TIME))
                .success(record.isVerified())
                .outcome(record.getOutcome() == null ? "—" : record.getOutcome())
                .liveness(formatScore(record.getLivenessScore()))
                .similarity(formatScore(record.getSimilarityScore()))
                .code(record.getSimaResponseCode() == null ? "—" : String.valueOf(record.getSimaResponseCode()))
                .beforeLabel(beforeLabel)
                .build();
    }

    private static boolean isBeforeReset(LocalDateTime createdAt, LocalDateTime resetAt) {
        return createdAt != null && resetAt != null && !createdAt.isAfter(resetAt);
    }

    private static String formatScore(Double score) {
        if (score == null) {
            return "—";
        }
        return String.format(Locale.US, "%.4f", score);
    }

    private String usersRedirect(int page, LocalDate from, LocalDate to, String phone, String sima, Long attempts) {
        StringBuilder url = new StringBuilder("redirect:/admin/users?page=").append(Math.max(page, 1));
        if (from != null) {
            url.append("&from=").append(from);
        }
        if (to != null) {
            url.append("&to=").append(to);
        }
        if (phone != null && !phone.isBlank()) {
            url.append("&phone=").append(URLEncoder.encode(phone.trim(), StandardCharsets.UTF_8));
        }
        url.append("&sima=").append(normalizeSimaFilter(sima));
        if (attempts != null) {
            url.append("&attempts=").append(attempts);
        }
        return url.toString();
    }

    private static final DateTimeFormatter SIMA_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


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

    private List<AuthUser> fetchUsers(LocalDate from, LocalDate to, String phone) {
        List<AuthUser> users = authUsersFeign.getUserList(
                from != null ? from.toString() : null,
                to != null ? to.toString() : null
        );
        if (users == null) {
            users = Collections.emptyList();
        }
        List<AuthUser> copy = new ArrayList<>(users);
        copy.sort(Comparator.comparing(AuthUser::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        if (phone != null && !phone.isBlank()) {
            String needle = phone.replaceAll("\\s+", "").toLowerCase();
            copy.removeIf(user -> {
                String p = user.getPhoneNumber();
                if (p == null) {
                    return true;
                }
                return !p.replaceAll("\\s+", "").toLowerCase().contains(needle);
            });
        }
        return copy;
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

    private static String boolCell(Boolean value) {
        if (value == null) {
            return "";
        }
        return Boolean.TRUE.equals(value) ? "true" : "false";
    }

    /**
     * tr: DB Frankfurt (UTC) zamanını Azerbaycan (UTC+4) için +4 saat kaydırarak formatlar.
     * en: Formats DB Frankfurt (UTC) time shifted +4 hours for Azerbaijan (UTC+4) display.
     */
    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? EXCEL_DATE_FORMAT.format(dateTime.plusHours(4)) : "";
    }
}
