package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.PromoPreviewResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Promo;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.PromoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 * tr: Telefon + yazılan kod. Müşterinin promo'su ve kodun durumu. Yazma yok, Redis yok.
 * en: Phone plus the typed code. The customer's promo and the code state. No write, no Redis.
 */
@Service
@RequiredArgsConstructor
public class PromoPreviewService {

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String DEACTIVATED = "DEACTIVATED";
    public static final String EXPIRED = "EXPIRED";
    public static final String NOT_STARTED = "NOT_STARTED";
    public static final String USABLE = "USABLE";

    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    private final CustomerRepository customerRepository;
    private final PromoRepository promoRepository;

    public PromoPreviewResponse preview(String phoneNumber, String promoCode) {
        String phone = normalizePhone(phoneNumber);
        String key = codeKey(promoCode);
        if (phone == null || key == null) {
            throw MissingFieldException.required("phoneNumber, promoCode");
        }
        Customer customer = customerRepository.findByPhoneNumber(phone);
        Promo typed = promoRepository.findByCodeKey(key).orElse(null);
        boolean hasPromo = customer != null && Boolean.TRUE.equals(customer.getHasPromo());
        return PromoPreviewResponse.builder()
                .hasPromo(hasPromo)
                .sameCode(hasPromo && sameCode(customer, key))
                .codeState(codeState(typed, LocalDate.now(BAKU)))
                .build();
    }

    private boolean sameCode(Customer customer, String key) {
        if (customer.getPromoCode() != null
                && key.equals(customer.getPromoCode().trim().toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (customer.getPromoId() == null) {
            return false;
        }
        return promoRepository.findById(customer.getPromoId())
                .map(promo -> key.equals(promo.getCodeKey()))
                .orElse(false);
    }

    static String codeState(Promo promo, LocalDate today) {
        if (promo == null) {
            return NOT_FOUND;
        }
        if (!Promo.ACTIVE.equals(promo.getStatus())) {
            return DEACTIVATED;
        }
        if (Boolean.TRUE.equals(promo.getExpired())) {
            return EXPIRED;
        }
        if (promo.getStartDate() == null || promo.getEndDate() == null || today == null) {
            return EXPIRED;
        }
        if (today.isAfter(promo.getEndDate())) {
            return EXPIRED;
        }
        if (today.isBefore(promo.getStartDate())) {
            return NOT_STARTED;
        }
        return USABLE;
    }

    static String codeKey(String promoCode) {
        if (promoCode == null) {
            return null;
        }
        String key = promoCode.trim().toLowerCase(Locale.ROOT);
        return key.isEmpty() ? null : key;
    }

    static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String phone = raw.trim().replace(" ", "").replace("-", "");
        if (phone.matches("0\\d{9}")) {
            phone = "+994" + phone.substring(1);
        }
        if (!phone.matches("\\+994\\d{9}")) {
            return null;
        }
        return phone;
    }
}
