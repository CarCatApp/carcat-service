package com.carland.carland_service.controller;

import com.carland.carland_service.idda.IddaPlateCheck;
import com.carland.carland_service.idda.IddaPlateCheckRepository;
import com.carland.carland_service.idda.IddaPlateCheckSpec;
import com.carland.carland_service.security.AdminAccessService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        String plateRaw = blankToNull(plate);
        String vinRaw = blankToNull(vin);
        Pageable pageable = PageRequest.of(
                pageIndex,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "queriedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<IddaPlateCheck> checks = iddaPlateCheckRepository.findAll(
                IddaPlateCheckSpec.filters(customerFilter, customerRejected, plateRaw, vinRaw),
                pageable);

        addPagination(model, checks.getTotalPages(), pageIndex);
        model.addAttribute("checks", checks);
        model.addAttribute("filterCustomerId", customerRaw);
        model.addAttribute("filterPlate", plateRaw);
        model.addAttribute("filterVin", vinRaw);
        return "idda";
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
}
