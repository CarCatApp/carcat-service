package com.carland.carland_service.dto.booking;

import lombok.Data;

import java.util.List;

/**
 * tr: Seçilen range ile book. Ad, fiyat, VIN ve plaka sunucuda arabadan və kataloqdan kopyalanır.
 * en: Book the chosen range. Name, price, VIN and plate are copied on the server from the car and catalog.
 */
@Data
public class BookingAppointmentRequest {
    Long rangeId;
    Long packageId;
    List<Long> individualServiceIds;
    String issue;
    Long carId;
}
