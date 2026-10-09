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
    String contactEmail;
    String instagram;
    String workingHours;
    String workingHoursWeekday;
    String workingHoursSaturday;
    String workingHoursSunday;
    String workingHoursWeekend;
    Boolean verified;
    String photo;
    Double rating;
    Long ratingCount;
}
