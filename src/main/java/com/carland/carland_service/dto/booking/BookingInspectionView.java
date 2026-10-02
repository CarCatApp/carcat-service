package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingInspectionView {
    Long id;
    Long bookingId;
    Long customerId;
    String customerName;
    String customerPhone;
    Long rangeId;
    String day;
    String slotStart;
    Long branchId;
    String branchName;
    Long partnerId;
    String ref;
    String message;
    String vin;
    Long carId;
    String plateNumber;
    String carBrand;
    String carModel;
    Integer carYear;
    String createdAt;
}
