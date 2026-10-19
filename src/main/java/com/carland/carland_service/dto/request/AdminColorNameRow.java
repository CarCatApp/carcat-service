package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Renk adı. colorId boşsa yeni satırdır. az / en / ru yalnız addır; hex ve sıra gelmez.
 * en: Color name. A missing colorId means a new row. az / en / ru are names only; hex and order are not sent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminColorNameRow {
    Long colorId;
    String az;
    String en;
    String ru;
}
