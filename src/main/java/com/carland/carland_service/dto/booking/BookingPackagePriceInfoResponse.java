package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPackagePriceInfoResponse {
    Long packageId;
    Integer price;
    String currency;
    String title;
    String subtitle;
    String description;
}
