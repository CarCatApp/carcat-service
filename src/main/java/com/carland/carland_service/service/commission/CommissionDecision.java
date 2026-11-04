package com.carland.carland_service.service.commission;

import java.time.LocalDate;

/**
 * tr: Bir VIN için donmuş karar.
 * en: Frozen decision for one VIN.
 */
public record CommissionDecision(String status, String outcome, LocalDate earliestServiceDate, String snapshot) {
}
