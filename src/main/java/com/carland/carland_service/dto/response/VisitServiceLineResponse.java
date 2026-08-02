package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * tr: Bir servis ziyaretindeki tek bir servis satırını (servis kodu, adı, grupları, maliyeti, sonraki servis bilgisi) temsil eden yanıt DTO'su.
 * en: Response DTO representing a single service line within a visit (service code, name, groups, cost, next service info).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitServiceLineResponse {
    private Integer serviceCode;
    private String universalServiceId;
    private String serviceName;
    private List<String> serviceGroups;
    private MoneyResponse cost;
    private LocalDate nextServiceDate;
    private Integer nextServiceMileage;
}
