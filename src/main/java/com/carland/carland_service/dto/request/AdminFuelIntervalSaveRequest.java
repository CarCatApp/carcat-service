package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Bir yakıt sətirinin km və ay aralığı.
 * en: Km and month interval of one fuel row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFuelIntervalSaveRequest {
    Long serviceId;
    Long intervalKm;
    Integer intervalMonth;
}
