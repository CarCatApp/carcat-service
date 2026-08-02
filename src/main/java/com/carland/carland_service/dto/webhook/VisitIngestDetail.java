package com.carland.carland_service.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: Partner webhook yeni ziyaret (ingest) akışında tek bir ziyaretin oluşturulma sonucunu ve satır detaylarını taşıyan DTO.
 * en: DTO carrying the creation result and line details of a single visit in the partner webhook new-visit (ingest) flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitIngestDetail {
    private Long partnerRecordId;
    private Long visitId;
    private boolean visitCreated;
    @Builder.Default
    private List<LineIngestDetail> lines = new ArrayList<>();
}
