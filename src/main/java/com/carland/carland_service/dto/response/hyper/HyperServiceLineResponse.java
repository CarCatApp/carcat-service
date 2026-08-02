package com.carland.carland_service.dto.response.hyper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * tr: Hyper API servis geçmişi kaydındaki tek bir servis satırını (servis kodu, adı, maliyeti, sonraki servis bilgisi) eşleyen yanıt DTO'su.
 * en: Response DTO mapping a single service line within a Hyper API service history record (service code, name, cost, next service info).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HyperServiceLineResponse {
    private Integer serviceCode;
    private String serviceName;
    private List<String> serviceGroups;
    private String universalServiceId;
    private HyperCostResponse cost;
    private LocalDate nextServiceDate;
    private Integer nextServiceMileage;
}
