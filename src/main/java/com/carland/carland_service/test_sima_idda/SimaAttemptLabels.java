package com.carland.carland_service.test_sima_idda;

/**
 * Admin / Excel display labels for SIMA KYC attempts.
 * Foreign: TRC = temporary (5-digit series), PRC = permanent (6-digit series).
 */
public final class SimaAttemptLabels {

    private SimaAttemptLabels() {
    }

    public static String kind(String channel, String documentType) {
        String ch = channel == null ? "" : channel.trim().toUpperCase();
        String doc = documentType == null ? "" : documentType.trim().toUpperCase();
        return switch (ch) {
            case "CITIZEN" -> "citizen";
            case "PASSPORT" -> "passport";
            case "FOREIGN" -> foreignKind(doc);
            default -> ch.isEmpty() ? "—" : ch.toLowerCase();
        };
    }

    private static String foreignKind(String documentType) {
        return switch (documentType) {
            case "TRC" -> "foreign/temp";
            case "PRC" -> "foreign/perm";
            case "ERP" -> "foreign/erp";
            default -> documentType.isEmpty() ? "foreign" : "foreign/" + documentType.toLowerCase();
        };
    }
}
