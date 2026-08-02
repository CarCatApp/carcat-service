package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: VIN numarasına göre sorgulanan araç servis geçmişini (kaynak ve kalem listesi) döndüren yanıt DTO'su.
 * en: Response DTO returning a car's service history looked up by VIN (source and list of items).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarVinServiceHistoryResponse {
    private String vin;
    private String source;
    private List<ServiceHistoryItemResponse> items;
}
