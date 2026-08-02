package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: VIN'e göre aracın ziyaret (visit) bazlı servis geçmişini özet ve kalem listesiyle döndüren üst seviye yanıt DTO'su.
 * en: Top-level response DTO returning a car's visit-based service history by VIN, with a summary and list of items.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitHistoryResponse {
    private String vin;
    private String source;
    private VisitHistorySummaryResponse summary;
    private List<VisitHistoryItemResponse> items;
}
