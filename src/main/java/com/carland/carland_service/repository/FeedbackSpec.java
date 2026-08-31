package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Feedback;
import com.carland.carland_service.entity.FeedbackPhoto;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: Admin feedback listesi filtreleri (Criteria; JPQL LIKE + page count Hibernate 6 hatasından kaçınır).
 * en: Admin feedback list filters (Criteria; avoids Hibernate 6 JPQL LIKE + page count bind bug).
 */
public final class FeedbackSpec {

    private FeedbackSpec() {
    }

    public static Specification<Feedback> filters(String type, String phone, boolean withPhoto) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (type != null && !type.isBlank()) {
                parts.add(cb.equal(cb.lower(root.get("type")), type.trim().toLowerCase()));
            }
            if (phone != null && !phone.isBlank()) {
                parts.add(cb.like(
                        cb.lower(cb.coalesce(root.get("customerPhone"), "")),
                        "%" + phone.trim().toLowerCase() + "%"));
            }
            if (withPhoto && query != null) {
                Subquery<Integer> exists = query.subquery(Integer.class);
                Root<FeedbackPhoto> photo = exists.from(FeedbackPhoto.class);
                exists.select(cb.literal(1));
                exists.where(cb.equal(photo.get("feedbackId"), root.get("feedbackId")));
                parts.add(cb.exists(exists));
            }
            if (parts.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }
}
