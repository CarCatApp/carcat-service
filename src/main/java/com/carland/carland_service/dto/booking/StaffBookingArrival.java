package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Müşteri rezervasyonu yazılınca açık partner ekranına giden satır.
 * en: The line sent to an open partner screen when a customer booking is saved.
 */
@Data
@Builder
public class StaffBookingArrival {
    Long bookingId;
    Long branchId;
    String brand;
    String model;
    String vin;
    String plateNumber;
    String customerName;
    String services;
}
