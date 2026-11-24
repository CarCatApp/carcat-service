package com.carland.carland_service.dto.booking;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Tamamlama pəncərəsində seçilə bilən bir şöbə paketi. Qiymət AZN (manat).
 * en: One branch package the completion dialog can select. Price is AZN (whole manat).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCompletionPackageOption {
    Long id;
    String name;
    /** Whole manat, same number stored on the package. */
    BigDecimal price;
    /** Non-periodic lines. Shown under the package name, not as cards. */
    List<String> infoLines;
    /** Periodic lines. Each becomes a locked package card. */
    List<StaffCompletionCardView> cards;
}
