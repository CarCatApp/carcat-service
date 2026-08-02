package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * tr: Araç servis geçmişi sorgularında dönen tek kayıtlık yanıt DTO'su (servis adı, tarih, km, tutar, servis merkezi).
 * en: Single-entry response DTO returned by car service history queries (service name, date, km, amount, service center).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceHistoryResponse {

    Long id;
    String serviceName;
    String actionType;
    LocalDate doneDate;
    Integer doneKm;
    BigDecimal serviceAmount;
    String serviceCenter;
    Long serviceCenterId;
}
