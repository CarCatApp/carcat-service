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
        /** branch_serviced_goods id. */
        Long goodId;
        /** Free-text product when no good is picked. */
        String productName;
        BigDecimal productQty;
        /** Product total; null → good unit price × qty. Ignored for package lines. */
        BigDecimal productAmount;
        /** Service price; ignored for package lines. */
        BigDecimal price;
        BigDecimal discountPercent;
        /** Manual final amount; null → (price + product) × (1 − discount%). */
        BigDecimal finalAmount;
        String notes;
    }
}
