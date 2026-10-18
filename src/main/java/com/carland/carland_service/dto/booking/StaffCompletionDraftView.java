package com.carland.carland_service.dto.booking;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: "Xidməti tamamla" pəncərəsinin başlanğıc məlumatı. Pullar AZN (qəpik deyil).
 * en: Initial data for the "Complete service" dialog. Money is AZN (not qəpik).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCompletionDraftView {
    Long bookingId;
    String ref;
    /** detailed | webhook. Webhook branches complete without the form. */
    String completionMode;
    /** Car mileage on file; the operator overwrites it with the mileage on arrival. */
    Long carMileage;
    String packageName;
    BigDecimal packagePrice;
    /** Package lines with periodic=false: written as info lines, no card. */
    List<String> packageInfoLines;
    /** Customer's təmir/yoxlanış text, if any. */
    String inspectionNote;
    /** Cards the dialog opens with: periodic package lines, booked individual services, inspection. */
    List<StaffCompletionCardView> cards;
    /** "Yeni xidmət → Dövri": individual services this branch has enabled. */
    List<StaffCompletionCardView> individualCatalog;
    /** Products by heading, with unit prices. */
    List<BookingBrandModelGroupView> goods;
}
