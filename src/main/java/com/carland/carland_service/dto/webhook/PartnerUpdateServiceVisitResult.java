package com.carland.carland_service.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: WebhookController servis ziyareti güncelleme işleminin sonucunu (güncellenen alan, satır ve parça sayıları) döndüren DTO.
 * en: DTO returning the result of the WebhookController service visit update operation (updated field, line and part counts).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerUpdateServiceVisitResult {
    private String vin;
    private String message;
    private Long partnerRecordId;
    private Long visitId;
    private int visitFieldsUpdated;
    private int linesUpdated;
    private int partsUpdated;
    @Builder.Default
    private List<LineUpdateDetail> lines = new ArrayList<>();
    @Builder.Default
    private List<PartUpdateDetail> parts = new ArrayList<>();
}
