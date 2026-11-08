package com.carland.carland_service.digitalhistory;

import com.carland.carland_service.entity.Car;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: Texniki sətirlər əlavə yoluna görə. Boş göstərilən dəyər «—» olur. Gizlənən sətir yazılmır.
 * en: Technical rows follow the add path. A shown empty value is «—». A hidden row is omitted.
 */
final class DigitalHistorySpecs {

    private DigitalHistorySpecs() {
    }

    static boolean platePath(Car car) {
        if (car.getIddaProvidedFields() != null && !car.getIddaProvidedFields().isEmpty()) {
            return true;
        }
        return car.getPath() != null && Car.PATH_PLATE.equalsIgnoreCase(car.getPath().trim());
    }

    static List<DigitalHistoryReport.SpecLine> rows(Input in, DigitalHistoryCopy copy) {
        List<DigitalHistoryReport.SpecLine> rows = new ArrayList<>();
        if (in.fromPlate()) {
            add(rows, copy.certificateNumber(), in.certificateNumber());
            add(rows, copy.certificateDate(), in.certificateDate());
        }
        add(rows, copy.plate(), in.plateNumber());
        add(rows, copy.brandModel(), in.brandModel());
        add(rows, copy.vin(), in.vin());
        add(rows, copy.year(), in.year());
        add(rows, copy.color(), in.color());
        add(rows, copy.volume(), in.volume());
        if (in.fromPlate()) {
            add(rows, copy.engineNumber(), in.engineNumber());
        }
        add(rows, copy.bodyType(), in.bodyType());
        return List.copyOf(rows);
    }

    static String note(Input in, DigitalHistoryCopy copy) {
        List<String> fields = new ArrayList<>();
        if (in.fromPlate()) {
            if (has(in.bodyType())) {
                fields.add(copy.bodyType());
            }
        } else {
            if (has(in.plateNumber())) {
                fields.add(copy.plate());
            }
            if (has(in.brandModel())) {
                fields.add(copy.brandModel());
            }
            if (has(in.year())) {
                fields.add(copy.year());
            }
            if (has(in.color())) {
                fields.add(copy.color());
            }
            if (has(in.volume())) {
                fields.add(copy.volume());
            }
            if (has(in.bodyType())) {
                fields.add(copy.bodyType());
            }
        }
        return copy.manualSentence(fields);
    }

    private static void add(List<DigitalHistoryReport.SpecLine> rows, String label, String value) {
        rows.add(new DigitalHistoryReport.SpecLine(label, has(value) ? value.trim() : "—"));
    }

    private static boolean has(String value) {
        return value != null && !value.isBlank();
    }

    record Input(
            boolean fromPlate,
            String certificateNumber,
            String certificateDate,
            String plateNumber,
            String brandModel,
            String vin,
            String year,
            String color,
            String volume,
            String engineNumber,
            String bodyType
    ) {
    }
}
