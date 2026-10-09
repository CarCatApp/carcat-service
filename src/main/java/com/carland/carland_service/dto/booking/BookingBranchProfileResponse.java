package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingBranchProfileResponse {
    Long branchId;
    String name;
    Long partnerId;
    String partnerName;
    Boolean verified;
    String logoUrl;
    String address;
    Double lat;
    Double lng;
    Double distanceKm;
    String workingHoursWeekday;
    String workingHoursSaturday;
    String workingHoursSunday;
    String workingHoursWeekend;
    Boolean open;
    List<BookingBranchProfileServiceView> services;
    List<BookingBranchProfileProductView> products;
}
