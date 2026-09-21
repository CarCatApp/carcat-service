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
    Long branchId;
    String name;
    String address;
    Double lat;
    Double lng;
    Boolean active;
    String contactPhone;
    String workingHours;
    String photo;
    Integer ratingCount;
}
