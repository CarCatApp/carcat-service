package com.carland.carland_service.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: WebhookController yeni servis ziyareti (ingest) işleminin sonucunu (oluşturulan/atlanan ziyaret, satır ve parça sayıları) döndüren DTO.
 * en: DTO returning the result of the WebhookController new service visit (ingest) operation (created/skipped visit, line and part counts).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerNewServiceVisitResult {
    private String vin;
    private String message;
    private int visitsCreated;
    private int visitsSkipped;
    private int linesCreated;
    private int linesSkipped;
    private int partsCreated;
    private int partsSkipped;
    @Builder.Default
    private List<VisitIngestDetail> visits = new ArrayList<>();
}
