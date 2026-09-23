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
public class BookingStaffPartnerView {
    Long id;
    String name;
    String logoUrl;
    Double rating;
    Long ratingCount;
    List<BookingBranchView> branches;
}
