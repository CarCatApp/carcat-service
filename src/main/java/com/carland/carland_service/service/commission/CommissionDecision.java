package com.carland.carland_service.service.commission;

import java.time.LocalDate;

/**
 * tr: Bir yoxlamanın nəticəsi. serviceDate son xidmət tarixidir.
 * en: Result of one check. serviceDate is the last service date.
 */
public record CommissionDecision(String status, String outcome, LocalDate serviceDate, String snapshot) {
}
