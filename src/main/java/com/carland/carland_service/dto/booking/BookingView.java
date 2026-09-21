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
public class BookingView {
    Long bookingId;
    String ref;
    String status;
    String bookingMode;
    Long branchId;
    Long slotId;
    String day;
    String start;
    String end;
    String timezone;
    String vin;
    Long carId;
    String startsAt;
    String branchName;
    String partnerName;
    List<String> serviceKeys;
    Integer priceMin;
    Integer priceMax;
    String currency;
    String unit;
    Integer unreadCount;
}
