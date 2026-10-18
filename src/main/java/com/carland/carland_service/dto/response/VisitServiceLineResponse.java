package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    /* Partner panel lines only (null for Hyper). cost = service + product before discount. */

    /** package | package_info | individual | one_time | inspection */
    private String kind;
    private LocalDate doneDate;
    private Integer doneKm;
    private MoneyResponse serviceAmount;
    private String productName;
    private BigDecimal productQty;
    private MoneyResponse productAmount;
    private BigDecimal discountPercent;
    private MoneyResponse finalAmount;
    private String notes;
}
