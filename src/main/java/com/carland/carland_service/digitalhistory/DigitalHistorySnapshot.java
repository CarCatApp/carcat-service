package com.carland.carland_service.digitalhistory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * tr: Buraxılış anının mətni. Şəkil burada yoxdur.
 * en: Text frozen at issue time. The photo is not stored here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DigitalHistorySnapshot(
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
        List<DigitalHistoryReport.VisitBlock> visits,
        String grandTotal,
        List<DigitalHistoryReport.CenterBlock> centers,
        String reportNo,
        String issuedAt,
        String lang,
        String mileageUpdatedOn,
        String centerCities,
        String technicalNote,
        List<DigitalHistoryReport.SpecLine> specs
) {
    static DigitalHistorySnapshot from(DigitalHistoryReport report) {
        return new DigitalHistorySnapshot(
                report.generatedOn(), report.brand(), report.model(), report.brandModel(),
                report.plate(), report.vin(), report.year(), report.color(),
                report.engineVolume(), report.engineNumber(), report.bodyType(),
                report.certificateNumber(), report.certificateDate(),
                report.mileage(), report.lastServiceDate(), report.serviceCount(), report.period(),
                report.centerCount(), report.visits(), report.grandTotal(), report.centers(),
                report.reportNo(), report.issuedAt(), report.lang(), report.mileageUpdatedOn(),
                report.centerCities(), report.technicalNote(), report.specs());
    }

    DigitalHistoryReport toReport(byte[] photo) {
        return new DigitalHistoryReport(
                generatedOn, brand, model, brandModel, plate, vin, year, color,
                engineVolume, engineNumber, bodyType, certificateNumber, certificateDate,
                mileage, lastServiceDate, serviceCount, period, centerCount,
                visits == null ? List.of() : visits,
                grandTotal,
                centers == null ? List.of() : centers,
                reportNo, issuedAt, lang, mileageUpdatedOn, centerCities, technicalNote,
                specs == null ? List.of() : specs,
                photo);
    }
}
