package com.carland.carland_service.dto.booking;

import lombok.Data;

import java.math.BigDecimal;
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
    /** tr: İsteğe bağlı. Doluysa booking'e olduğu gibi yazılır. / en: Optional. When present it is stored as sent. */
    String promoCode;
    String discountType;
    BigDecimal discountValue;
}
