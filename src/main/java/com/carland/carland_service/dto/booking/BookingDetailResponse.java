package com.carland.carland_service.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    String logoUrl;
    Long slotId;
    String day;
    String start;
    String end;
    String startsAt;
    String timezone;
    BookingCarView car;
    @JsonProperty("package")
    BookingPackageView bookedPackage;
    List<BookingServiceLineView> services;
    List<BookingLineView> items;
    Integer priceMin;
    Integer priceMax;
    String currency;
    String unit;
    Integer unreadCount;
    String canceledBy;
    BookingCanceledReasonView canceledReason;
}
