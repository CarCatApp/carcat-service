package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Rengi bir dilde yeni sıraya taşır. position 1’den başlar.
 * en: Moves a color to a new position in one language. position is 1-based.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminColorSortMoveRequest {
    String lang;
    Long colorId;
    Integer position;
}
