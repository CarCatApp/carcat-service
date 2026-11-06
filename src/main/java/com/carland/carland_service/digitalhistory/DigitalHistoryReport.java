package com.carland.carland_service.digitalhistory;

import java.util.List;

/**
 * tr: PDF'e basılan düz metin. Entity taşınmaz. Boş alan {@code ""}.
 *     {@code photo} aracın hazır fotoğrafıdır; yoksa null. Loga yazılmaz.
 * en: Plain text printed on the PDF. No entities. A missing value is {@code ""}.
 *     {@code photo} is the car's ready photo, or null. It is not logged.
 */
public record DigitalHistoryReport(
        String generatedOn,
        String brand,
        String model,
        String brandModel,
        String plate,
        String vin,
        String year,
        String color,
        String engineVolume,
        String engineNumber,
        String bodyType,
        String certificateNumber,
        String certificateDate,
        String mileage,
        String lastServiceDate,
        int serviceCount,
        String period,
        int centerCount,
        List<VisitBlock> visits,
        String grandTotal,
        List<CenterBlock> centers,
        byte[] photo
) {
    public record VisitBlock(
            String mileage,
            String date,
            String centerName,
            String address,
            String label,
            String packageName,
            List<String> packageLines,
            List<String> individualLines,
            String inspectionNote,
            List<String> inspectionLines,
            List<String> plainLines,
            String total
    ) {
    }

    public record CenterBlock(
            String name,
            String address,
            String phone,
            String email,
            int visits,
            String total
    ) {
    }
}
