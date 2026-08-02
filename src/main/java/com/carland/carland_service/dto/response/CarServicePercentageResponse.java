package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


/**
 * tr: Bir aracın servis kalemi bazında bakım yüzdesi durumunu (km/ay yüzdesi, kalan km, son/sonraki servis) döndüren yanıt DTO'su (percentage akışı).
 * en: Response DTO returning a car's per-service maintenance percentage status (km/month percentage, remaining km, last/next service) in the percentage flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarServicePercentageResponse {
    Long percentageId;
    Long serviceId;
    String serviceName;
    String serviceNameAz;
    String serviceNameEn;
    String serviceNameRu;
    String actionType;

    Long intervalKm;
    Integer intervalMonth;

    Integer kmPercentage;
    Integer monthPercentage;
    Integer monthPercentageDigit; // yeni faiz ile tarix faizini gosteren field elave etmisem
    Integer remainingKm;
    String  remainingMonths;

    Integer lastServiceKm;
    String lastServiceDate;

    Integer nextServiceKm;
    String nextServiceDate;
    String status;
    Boolean editable;
    String servicedStatus;
    boolean important;
}
