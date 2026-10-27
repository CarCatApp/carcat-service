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
public class BookingCarePackageView {
    Long id;
    String name;
    /** Partner panelde adın altındaki metin. Boşsa null. */
    String packageInfo;
    Integer price;
    String currency;
    /** Gruplarda görünen açık xidmət sayısı. Filtresi olmayan satır sayılmaz. */
    Integer count;
    List<BookingCarePackageGroupView> groups;
}
