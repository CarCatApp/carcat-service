package com.carland.carland_service.dto.response.hyper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * tr: Hyper API'den gelen tek bir servis geçmişi kaydını (servis satırları, parçalar, maliyet, sonraki servis) eşleyen yanıt DTO'su.
 * en: Response DTO mapping a single service history record from the Hyper API (service lines, parts, cost, next service).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HyperServiceHistoryItemResponse {
    private Long recordId;
    private String serviceType;
    private List<String> serviceGroups;
    private LocalDate lastServiceDate;
    private Integer lastServiceMileage;
    private List<HyperServiceLineResponse> services;
    private List<HyperServicePartResponse> parts;
    private HyperCostResponse cost;
    private HyperCostResponse finalCost;
    private LocalDate nextServiceDate;
    private Integer nextServiceMileage;
    private String invoiceNumber;
    private String dealer;
}
