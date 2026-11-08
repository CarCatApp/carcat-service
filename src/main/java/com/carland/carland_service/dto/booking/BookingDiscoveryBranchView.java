package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDiscoveryBranchView {
    Long partnerId;
    String partnerName;
    Long branchId;
    String name;
    String address;
    Double lat;
    Double lng;
    Boolean active;
    String contactPhone;
    String workingHours;
    String workingHoursWeekday;
    String workingHoursSaturday;
    String workingHoursSunday;
    String workingHoursWeekend;
    Boolean verified;
    String photo;
    Double rating;
    Long ratingCount;
    /**
     * tr: Partner paneldə təmir və diaqnostika. Açıqsa true, bağlıysa və ya satır yoxsa false.
     * en: Partner-panel repair and diagnostics. True when on, false when off or when no row exists.
     */
    Boolean inspectionRepair;
}
