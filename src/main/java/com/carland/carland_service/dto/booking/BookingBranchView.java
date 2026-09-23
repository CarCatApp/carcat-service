package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingBranchView {
    Long id;
    Long partnerId;
    String partnerName;
    String name;
    String address;
    Double lat;
    Double lng;
    Boolean active;
    Boolean partnerActive;
    String contactPhone;
    String workingHours;
    String photo;
    Double rating;
    Long ratingCount;
}
