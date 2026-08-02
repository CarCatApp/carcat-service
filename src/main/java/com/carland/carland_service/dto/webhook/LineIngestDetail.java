package com.carland.carland_service.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Partner webhook yeni ziyaret (ingest) akışında tek bir servis satırının oluşturulma sonucunu taşıyan DTO.
 * en: DTO carrying the creation result of a single service line in the partner webhook new-visit (ingest) flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineIngestDetail {
    private Integer serviceCode;
    private Long lineId;
    private boolean created;
}
