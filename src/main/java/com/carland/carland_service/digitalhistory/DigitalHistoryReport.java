package com.carland.carland_service.digitalhistory;

import java.util.List;

/**
 * tr: PDF-ə basılan düz mətn. Entity taşınmaz. Boş alan {@code ""}.
 *     {@code photo} aracın hazır fotoğrafıdır; yoxlama snapshot-ına yazılmır.
 * en: Plain text printed on the PDF. No entities. A missing value is {@code ""}.
 *     {@code photo} is the car's ready photo and is not stored in the verify snapshot.
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
        String reportNo,
        String issuedAt,
        String lang,
        String mileageUpdatedOn,
        String centerCities,
        String technicalNote,
        List<SpecLine> specs,
        byte[] photo
) {
    public DigitalHistoryReport withIssue(String reportNo, String issuedAt) {
        return new DigitalHistoryReport(
                generatedOn, brand, model, brandModel, plate, vin, year, color,
                engineVolume, engineNumber, bodyType, certificateNumber, certificateDate,
                mileage, lastServiceDate, serviceCount, period, centerCount,
                visits, grandTotal, centers,
                reportNo, issuedAt, lang, mileageUpdatedOn, centerCities, technicalNote, specs,
                photo);
    }

    public DigitalHistoryReport withPhoto(byte[] photo) {
        return new DigitalHistoryReport(
                generatedOn, brand, model, brandModel, plate, vin, year, color,
                engineVolume, engineNumber, bodyType, certificateNumber, certificateDate,
                mileage, lastServiceDate, serviceCount, period, centerCount,
                visits, grandTotal, centers,
                reportNo, issuedAt, lang, mileageUpdatedOn, centerCities, technicalNote, specs,
                photo);
    }

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
            String total,
            boolean repair
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

    public record SpecLine(String label, String value) {
    }
}
