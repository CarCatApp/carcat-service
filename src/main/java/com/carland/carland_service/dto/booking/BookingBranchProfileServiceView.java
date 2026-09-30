package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingBranchProfileServiceView {
    Long id;
    String code;
    String name;
    /** Şubedeki aktif care paket sayısı. Yalnız routine kartında dolar. */
    Integer packageCount;
    /** Partner-ui yazınca dolacak. Kaynak yokken boş. */
    Integer serviceCount;
}
