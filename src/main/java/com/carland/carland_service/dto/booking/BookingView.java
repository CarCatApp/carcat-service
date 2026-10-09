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
    Long partnerId;
    Long slotId;
    String day;
    String start;
    String end;
    String timezone;
    String vin;
    Long carId;
    String customerName;
    String phone;
    Long customerUserId;
    String plateNumber;
    String carBrand;
    String carModel;
    /** tr: cars satırının ham alanları. / en: Raw columns from the cars row. */
    CarResponseForSlotPanel car;
    String cancelReasonCode;
    String cancelNote;
    BookingCanceledReasonView canceledReason;
    String startsAt;
    String branchName;
    String branchAddress;
    String partnerName;
    String logoUrl;
    List<String> serviceKeys;
    String packageName;
    Integer packagePrice;
    List<BookingServiceLineView> individualServices;
    BookingInspectionView inspection;
    String serviceLabel;
    Integer priceMin;
    Integer priceMax;
    String currency;
    String unit;
    Integer unreadCount;
}
