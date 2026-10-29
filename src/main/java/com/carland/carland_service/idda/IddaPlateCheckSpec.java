package com.carland.carland_service.idda;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * tr: Admin IDDA listesi. customerId tam eşleşir. Plaka anahtarı ve VIN içinde aranır.
 * en: Admin IDDA list. customerId is exact. Plate key and VIN are contains searches.
 */
public final class IddaPlateCheckSpec {

    private IddaPlateCheckSpec() {
    }

    public static Specification<IddaPlateCheck> filters(
            Long customerId,
            boolean customerRejected,
            String plate,
            String vin
    ) {
        return (root, query, cb) -> {
            if (customerRejected) {
                return cb.disjunction();
            }
            List<Predicate> parts = new ArrayList<>();
            if (customerId != null) {
                parts.add(cb.equal(root.get("querierUserId"), customerId));
            }
            String plateKey = plateNeedle(plate);
            if (plateKey != null) {
                parts.add(cb.like(
                        cb.lower(cb.coalesce(root.get("plateKey"), "")),
                        "%" + plateKey.toLowerCase(Locale.ROOT) + "%"));
            }
            String vinKey = vinNeedle(vin);
            if (vinKey != null) {
                parts.add(cb.like(
                        cb.lower(cb.coalesce(root.get("vin"), "")),
                        "%" + vinKey.toLowerCase(Locale.ROOT) + "%"));
            }
            if (parts.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }

    /** tr: 77-BU-669 ile 77BU669 aynı iğnedir. / en: 77-BU-669 and 77BU669 are the same needle. */
    public static String plateNeedle(String plate) {
        if (plate == null || plate.isBlank()) {
            return null;
        }
        String key = IddaPlateKey.of(plate);
        return key.isEmpty() ? null : key;
    }

    public static String vinNeedle(String vin) {
        if (vin == null || vin.isBlank()) {
            return null;
        }
        String cleaned = vin.replace(" ", "").trim();
        return cleaned.isEmpty() ? null : cleaned.toUpperCase(Locale.ROOT);
    }
}
