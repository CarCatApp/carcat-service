package com.carland.carland_service.service.clientevent;

import com.carland.carland_service.entity.ClientErrorEvent;
import com.carland.carland_service.entity.ClientErrorIssue;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class ClientEventSpecs {

    private ClientEventSpecs() {
    }

    static Specification<ClientErrorEvent> events(Long userId, Long issueId, String tab,
                                                  LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (userId != null) {
                parts.add(cb.equal(root.get("userId"), userId));
            }
            if (issueId != null) {
                parts.add(cb.equal(root.get("issueId"), issueId));
            }
            if (from != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                parts.add(cb.lessThan(root.get("occurredAt"), to));
            }
            if ("fatal".equals(tab)) {
                parts.add(cb.equal(root.get("eventType"), "fatal"));
            } else if ("nonfatal".equals(tab)) {
                parts.add(cb.equal(root.get("eventType"), "nonFatal"));
            } else if ("server".equals(tab)) {
                parts.add(cb.equal(root.get("eventType"), "apiError"));
                parts.add(cb.greaterThanOrEqualTo(root.get("statusCode"), 500));
            } else if ("client".equals(tab)) {
                parts.add(cb.equal(root.get("eventType"), "apiError"));
                parts.add(cb.or(
                        cb.and(
                                cb.greaterThanOrEqualTo(root.get("statusCode"), 400),
                                cb.lessThan(root.get("statusCode"), 500)),
                        cb.isNull(root.get("statusCode"))));
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }

    static Specification<ClientErrorIssue> issues(String eventType, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (eventType != null && !eventType.isBlank() && !"all".equals(eventType)) {
                parts.add(cb.equal(root.get("eventType"), eventType));
            }
            if (from != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("lastSeen"), from));
            }
            if (to != null) {
                parts.add(cb.lessThan(root.get("lastSeen"), to));
            }
            if (parts.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }
}
