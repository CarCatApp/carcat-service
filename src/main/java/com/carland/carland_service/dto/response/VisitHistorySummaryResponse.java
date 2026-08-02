package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Ziyaret geçmişinin özetini (toplam servis sayısı ve toplam tutar) döndüren yanıt DTO'su; VisitHistoryResponse içinde kullanılır.
 * en: Response DTO returning the visit history summary (total service count and total amount); used inside VisitHistoryResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitHistorySummaryResponse {
    private int serviceCount;
    private MoneyResponse totalAmount;
}
