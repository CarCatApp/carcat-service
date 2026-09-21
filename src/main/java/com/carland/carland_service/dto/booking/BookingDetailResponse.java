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
public class BookingDetailResponse {
    Long bookingId;
    String ref;
    String status;
    String bookingMode;
    Long branchId;
    String branchName;
    String branchAddress;
    String partnerName;
    Long slotId;
    String day;
    String start;
    String end;
    String startsAt;
    String timezone;
    BookingCarView car;
    List<BookingLineView> items;
    Integer priceMin;
    Integer priceMax;
    String currency;
    String unit;
    Integer unreadCount;
    Object canceledReason;
}
