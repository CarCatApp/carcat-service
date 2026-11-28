package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.BannerItem;
import com.carland.carland_service.entity.Banner;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.service.AdminBannerService;
import com.carland.carland_service.service.AppAnnouncementHeaders;
import com.carland.carland_service.service.HyperBranchLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * tr: Mobil banner listesi. Kimlik doğrulamaz. Boş sonuç {@code 200 []}.
 *     showing=true ve status=active olanlar, Accept-Language az/en/ru. Boş veya bilinmeyen dil az.
 *     expireDate Bakü gününden önceyse showing kapanır, sonra liste döner.
 *     hasPromo true değilse type=promo düşer.
 *     Carousel'da görünen arabanın carId'si query'de gelir. O araba köhnə Hyper ise
 *     başqa servisin branchId'si düşer.
 * en: Mobile banner list. No authentication. Empty result is {@code 200 []}.
 *     showing=true and status=active rows, Accept-Language az/en/ru. A blank or unknown language is az.
 *     An expireDate before today's Baku date turns showing off, then the list returns.
 *     A type=promo banner is omitted unless the customer hasPromo is true.
 *     The carousel sends the visible car as carId. An old Hyper car drops a branchId that is not Hyper.
 */
@Tag(name = "banners", description = "Public banner list. No JWT. Empty list is 200 [].")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {

    static final String JSON_UTF8 = "application/json;charset=UTF-8";

    private final AdminBannerService adminBannerService;
    private final HyperBranchLockService hyperBranchLockService;
    private final CustomerRepository customerRepository;

    @Operation(summary = "Visible banners",
            description = "Public. No headers required. Every showing=true and status=active banner "
                    + "for Accept-Language az, en, or ru (blank or unknown is az), "
                    + "ordered by sortOrder then newer id. Never 401. Empty list is 200 []. "
                    + "An expireDate before today's Baku date sets showing=false before the list returns. "
                    + "The expire date itself still shows. A blank expireDate never expires. "
                    + "type=promo is returned only when X-User-Id belongs to a customer whose hasPromo is true. "
                    + "Query carId is the car visible in the carousel, with X-User-Id. "
                    + "An old Hyper car omits banners whose branchId is not Hyper. "
                    + "Without carId the full list stays. Banners with an empty branchId stay.")
    @GetMapping(produces = JSON_UTF8)
    public ResponseEntity<List<BannerItem>> list(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(required = false) Long carId) {
        String lang = AppAnnouncementHeaders.language(acceptLanguage);
        List<BannerItem> items = hyperOnly(carId, userIdHeader)
                ? adminBannerService.visibleForOldHyper(lang)
                : adminBannerService.visible(lang);
        if (items == null) {
            items = List.of();
        }
        if (!hasPromo(userIdHeader)) {
            items = items.stream()
                    .filter(item -> item == null || !Banner.PROMO.equals(item.getType()))
                    .toList();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(JSON_UTF8))
                .cacheControl(CacheControl.noStore())
                .body(items);
    }

    /**
     * tr: Tek banner. Liste ile aynı nesne. Kapalı, silinmiş veya promo ve hasPromo değilse 404.
     * en: One banner. Same object as the list. 404 when closed, missing, or a promo the customer cannot see.
     */
    @Operation(summary = "One visible banner",
            description = "Public. Same fields as the list item, including promoId. "
                    + "404 when the banner is missing, not active, or not showing. "
                    + "A type=promo banner is 404 unless X-User-Id has hasPromo true.")
    @GetMapping(value = "/{id}", produces = JSON_UTF8)
    public ResponseEntity<BannerItem> one(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        BannerItem item = adminBannerService.visibleById(id);
        if (item != null && Banner.PROMO.equals(item.getType()) && !hasPromo(userIdHeader)) {
            throw new ResourceNotFoundException("banner not found");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(JSON_UTF8))
                .cacheControl(CacheControl.noStore())
                .body(item);
    }

    private boolean hasPromo(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return false;
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            return false;
        }
        Customer customer = customerRepository.findByUserId(userId);
        return customer != null && Boolean.TRUE.equals(customer.getHasPromo());
    }

    private boolean hyperOnly(Long carId, String userIdHeader) {
        if (carId == null) {
            return false;
        }
        return hyperBranchLockService.locked(carId, parseUserId(userIdHeader));
    }

    private static Long parseUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw MissingFieldException.required("X-User-Id");
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw MissingFieldException.required("X-User-Id");
        }
    }
}
