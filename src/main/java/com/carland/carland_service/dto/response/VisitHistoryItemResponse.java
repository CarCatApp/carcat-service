package com.carland.carland_service.dto.response;

import com.carland.carland_service.dto.response.PartnerDataResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * tr: Ziyaret (visit) geçmişindeki tek bir servis ziyaretini (tarih, km, servis satırları, parçalar, tutar) temsil eden yanıt DTO'su; VisitHistoryResponse içinde döner.
 * en: Response DTO representing a single service visit in the visit history (date, mileage, service lines, parts, amount); returned inside VisitHistoryResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitHistoryItemResponse {
    private Long id;
    private Long partnerRecordId;
    private String type;
    private List<String> serviceGroups;
    private List<VisitServiceLineResponse> services;
    private LocalDate date;
    private Integer mileage;
    /** Same as {@link #partner}.{@code id} — kept for mobile contract compatibility. */
    private Long serviceCenterId;
    private String serviceCenterName;
    private PartnerDataResponse partner;
    private String dealer;
    private MoneyResponse amount;
    /** Pre-discount visit cost from Hyper {@code cost}. */
    private MoneyResponse cost;
    private String invoiceNumber;
    private List<VisitPartResponse> parts;
}
