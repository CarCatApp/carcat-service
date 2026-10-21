package com.carland.carland_service.dto.response;

import com.carland.carland_service.dto.response.PartnerDataResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    /*
     * GEÇİCİ / TEMPORARY — Hyper partner 1 refactor.
     * Webhook ve Hyper GET şube göndermez. Partner 1'in tek şubesi branches.id = 10
     * (Hyper Service, Babək pr. 43A). serviceCenterId 1 ise cevapta bu şube yazılır.
     * visits.branch_id güncellenmez. İkinci Hyper şubesi açılınca veya partner 1
     * refactor edilince bu alanların doldurulduğu bağ kaldırılacak.
     * Hyper webhook/GET sends no branch. Partner 1's only branch is branches.id = 10.
     * When serviceCenterId is 1 the response shows that branch. visits.branch_id is not updated.
     * Remove the binding when a second Hyper branch appears or partner 1 is refactored.
     */
    private Long branchId;
    private String branchName;
    private Double lat;
    private Double lng;
    private MoneyResponse amount;
    /** Pre-discount visit cost from Hyper {@code cost}. */
    private MoneyResponse cost;
    private String invoiceNumber;
    private List<VisitPartResponse> parts;

    /*
     * Partner panel ("Xidməti tamamla") visits only. Mobile "Service summary":
     * Package = packageName/packagePrice, Individual services = services[kind in (package, individual, one_time)],
     * Products = parts, Inspection & repair = inspectionNote + services[kind = inspection],
     * Subtotal = cost, Discount = discount, Total = amount.
     */

    /** "panel" for partner-panel visits; null for Hyper. */
    private String source;
    private String packageName;
    private MoneyResponse packagePrice;
    /** Overall % applied after line discounts. */
    private BigDecimal discountPercent;
    /** cost − amount (all discounts together). */
    private MoneyResponse discount;
    private String inspectionNote;
}
