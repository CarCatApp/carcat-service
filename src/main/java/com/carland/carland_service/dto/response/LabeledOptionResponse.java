package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Dil destekli seçenek satırı (çekiş, transmisyon, kayış/zincir).
 * en: Localized option row (drive, transmission, belt/chain).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabeledOptionResponse {
    /** Stable code posted back by mobile ({@code FWD}, {@code AT4}, {@code BELT}, …). */
    private String code;
    private String label;
}
