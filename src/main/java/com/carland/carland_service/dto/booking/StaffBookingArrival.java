package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * tr: Müşteri rezervasyonu yazılınca açık partner ekranına giden satır.
 * en: The line sent to an open partner screen when a customer booking is saved.
 */
@Data
@Builder
public class StaffBookingArrival {
    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    Long bookingId;
    Long branchId;
    String ref;
    String bookingMode;
    String status;
    String day;
    String start;
    String end;
    String brand;
    String model;
    String vin;
    String plateNumber;
    String customerName;
    String phone;
    String packageName;
    Integer packagePrice;
    List<String> serviceNames;
    String issue;
    Integer priceMin;
    Integer priceMax;
    String createdAt;
    String pendingExpiresAt;

    public static String stamp(OffsetDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZoneSameInstant(BAKU).format(STAMP);
    }
}
