package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * tr: Servis geçmişindeki tek bir kaydı (servis adı, tarih, km, servis merkezi, parçalar, tutar) temsil eden yanıt DTO'su.
 * en: Response DTO representing a single service history entry (service name, date, km, service center, parts, amount).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceHistoryItemResponse {
    private Long id;
    private String serviceName;
    private List<String> actionType;
    private LocalDate doneDate;
    private Integer doneKM;
    private String serviceCenter;
    private Long serviceCenterId;
    private PartnerDataResponse partner;
    private BigDecimal serviceAmount;
    private String dealer;
    private List<ServiceHistoryPartResponse> parts;
    private LocalDate nextServiceDate;
    private Integer nextServiceMileage;
}
