package com.carland.carland_service.dto.booking;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Tamamlama pəncərəsində bir kart (və ya "Dövri" siyahısında bir sətir).
 * en: One card in the completion dialog (or one row in the "Dövri" list).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCompletionCardView {
    /** package | individual | one_time | inspection — see VisitLineKind. */
    String kind;
    Long offeredServiceId;
    Long individualServiceId;
    String title;
    /** Package group (Dəyişdirmə, …) for package lines. */
    String groupTitle;
    /** HyperServiceMapping name used for reminders, or null. */
    String maintenanceCode;
    /** True for package lines: price fields are locked ("paket qiymətinə daxildir"). */
    boolean priceIncluded;
    /** Branch price range (AZN) for individual services. */
    BigDecimal priceMin;
    BigDecimal priceMax;
}
