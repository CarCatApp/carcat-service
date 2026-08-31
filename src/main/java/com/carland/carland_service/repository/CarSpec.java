package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Car;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: Admin araç listesi filtreleri (Criteria; JPQL LIKE + page count Hibernate 6 hatasından kaçınır).
 * en: Admin car list filters (Criteria; avoids Hibernate 6 JPQL LIKE + page count bind bug).
 */
public final class CarSpec {

    private CarSpec() {
    }

    public static Specification<Car> filters(Long userId, String vin) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (userId != null) {
                parts.add(cb.equal(root.get("customer").get("userId"), userId));
            }
            if (vin != null && !vin.isBlank()) {
                String needle = "%" + vin.trim().toLowerCase() + "%";
                parts.add(cb.like(cb.lower(cb.coalesce(root.get("vin"), "")), needle));
            }
            if (parts.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }
}
