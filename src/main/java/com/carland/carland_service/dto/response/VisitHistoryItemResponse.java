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
     * Detailed panel ziyareti: visits.branch_id doluysa bu alanlar o şubedir.
     * Hyper GET/webhook şube göndermez; branch_id boş ve serviceCenterId 1 ise
     * geçici olarak branches.id = 10 (Hyper Service, Babək pr. 43A) yazılır.
     * A detailed panel visit with visits.branch_id fills these fields from that branch.
     * Hyper GET/webhook sends no branch; an empty branch_id with serviceCenterId 1
     * still shows branches.id = 10 until that temporary binding is removed.
     */
    private Long branchId;
    private String branchName;
    /** {@code branches.address} of the same branch as {@link #branchId}. */
    private String branchAddress;
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
