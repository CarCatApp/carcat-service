package com.carland.carland_service.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: "Xidməti tamamla" gövdəsi. Boş gövdə = köhnə davranış (yalnız status). Pullar AZN.
 * en: "Complete service" body. No body = old behaviour (status only). Money is AZN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCompleteRequest {
    /** Visit date. Defaults to today (branch time zone). */
    LocalDate date;
    /** Car mileage on arrival. Required. */
    Integer mileage;
    /** Overall discount % applied after line discounts (0–100). */
    BigDecimal discountPercent;
    /** One operator note for the whole visit. */
    String notes;
    List<StaffCompleteLine> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffCompleteLine {
        /** package | individual | one_time | inspection */
        String kind;
        Long offeredServiceId;
        Long individualServiceId;
        /** Required for one_time / inspection. Ignored for catalog lines (title comes from the catalog). */
        String title;
        LocalDate date;
        Integer mileage;
        LocalDate nextDate;
        Integer nextMileage;
        /** brand_models id. */
        Long brandModelId;
        /** Free-text / overridden product name. */
        String productName;
        BigDecimal productQty;
        /** Product total before product discount; null → brand unit price × qty. Ignored for package lines. */
        BigDecimal productAmount;
        /** Product-only discount % (0–100). */
        BigDecimal productDiscountPercent;
        /** Manual product final; null → productAmount × (1 − productDiscount%). */
        BigDecimal productFinalAmount;
        /** Service price; ignored for package lines. */
        BigDecimal price;
        /** Service-only discount % (0–100). */
        BigDecimal discountPercent;
        /** Manual service final; null → price × (1 − discount%). */
        BigDecimal finalAmount;
    }
}
