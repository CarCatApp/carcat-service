package com.carland.carland_service.service.impl;

import com.carland.carland_service.feign.NhtsaFeign;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tr: NHTSA VIN çözümleme API'sini (Feign) kullanarak VIN'den araç bilgilerini çıkaran servis; marka, model, yıl, kasa tipi, şanzıman, motor hacmi ve motor/yakıt tipini haritalar.
 * en: Service extracting vehicle information from a VIN using the NHTSA VIN decoding API (via Feign); maps brand, model, year, body type, transmission, engine volume, and engine/fuel type.
 */
@Service
@RequiredArgsConstructor
public class VinService {

    private final NhtsaFeign nhtsaFeign;

    /**
     * tr: Verilen VIN'i NHTSA API'sine gönderir ve ham JSON cevabını Map olarak döner.
     * en: Sends the given VIN to the NHTSA API and returns the raw JSON response as a Map.
     */
    public Map<String, Object> decodeVin(String vin) {
        return nhtsaFeign.decodeVin(vin, "json");
    }

    /**
     * tr: VIN'i çözüp NHTSA cevabından seçili alanları (brand, model, modelYear, bodyType, transmissionType, engineVolume cc cinsinden, engineType) String haritası olarak çıkarır; motor tipini elektriklenme seviyesi ve yakıt tiplerinden türetir.
     * en: Decodes the VIN and extracts selected fields from the NHTSA response (brand, model, modelYear, bodyType, transmissionType, engineVolume in cc, engineType) as a String map; derives the engine type from the electrification level and fuel types.
     */
    public Map<String, String> extractFieldsFromVin(String vin) {
        Map<String, Object> response = decodeVin(vin);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) response.get("Results");

        Map<String, String> output = new HashMap<>();

        String electrificationLevel = null;
        String primaryFuel = null;
        String secondaryFuel = null;

        for (Map<String, Object> item : results) {
            String variable = (String) item.get("Variable");
            String value = item.get("Value") != null
                    ? item.get("Value").toString()
                    : null;

            switch (variable) {
                case "Make" -> output.put("brand", value);

                case "Model" -> output.put("model", value);

                case "Model Year" -> output.put("modelYear", value);

                case "Body Class" -> output.put("bodyType", value);

                case "Transmission Style" -> output.put("transmissionType", value);

                case "Displacement (L)" -> {
                    Integer cc = parseEngineVolumeCc(value);
                    output.put("engineVolume", cc != null ? String.valueOf(cc) : value);
                }

                case "Fuel Type - Primary" -> primaryFuel = value;

                case "Fuel Type - Secondary" -> secondaryFuel = value;

                case "Electrification Level" -> electrificationLevel = value;
            }
        }

        String engineType = mapEngineType(
                electrificationLevel,
                primaryFuel,
                secondaryFuel
        );

        if (engineType != null) {
            output.put("engineType", engineType);
        }

        return output;
    }

    private String mapEngineType(String electrificationLevel,
                                 String primaryFuel,
                                 String secondaryFuel) {

        String electrification = electrificationLevel == null
                ? ""
                : electrificationLevel.toLowerCase().trim();

        String primary = primaryFuel == null
                ? ""
                : primaryFuel.toLowerCase().trim();

        String secondary = secondaryFuel == null
                ? ""
                : secondaryFuel.toLowerCase().trim();

        // Plug-in Hybrid
        if (electrification.contains("plug-in")
                || electrification.contains("plug in")
                || electrification.contains("phev")) {
            return "Plug-in Hybrid";
        }

        // Diesel Hybrid
        if (electrification.contains("diesel hybrid")
                || (primary.contains("diesel") && secondary.contains("electric"))) {
            return "Diesel Hybrid";
        }

        // Hybrid (HEV)
        if (electrification.contains("hybrid")
                || (primary.contains("gasoline") && secondary.contains("electric"))) {
            return "Hybrid";
        }

        // Electric (BEV)
        if (electrification.contains("battery electric")
                || electrification.contains("bev")
                || (electrification.contains("electric") && !electrification.contains("hybrid"))
                || primary.equals("electric")
                || primary.contains("battery electric")
                || (primary.isBlank() && secondary.equals("electric"))) {
            return "Electric";
        }

        // LPG / CNG
        if (primary.contains("lpg")
                || primary.contains("cng")
                || primary.contains("natural gas")) {
            return "Gas (LPG / CNG)";
        }

        // Diesel
        if (primary.contains("diesel")) {
            return "Diesel";
        }

        // Petrol
        if (primary.contains("gasoline")
                || primary.contains("petrol")) {
            return "Petrol (Gasoline)";
        }

        return null;
    }
    /**
     * NHTSA "Displacement (L)" is usually liters (1.5, 2.0).
     * Sometimes whole liters without decimals (5 → 5L).
     * Values already in cc/ml are typically 3–4 digits (890, 1200, 3500).
     *
     * tr: NHTSA'dan gelen motor hacmi değerini cc'ye çevirir: ondalıklı değerler litre kabul edilip 1000 ile çarpılır, 3 haneden kısa tam sayılar litre sayılır, diğerleri zaten cc kabul edilir. null/boş veya sayı olmayan girişte null döner.
     * en: Converts the engine displacement value from NHTSA to cc: decimal values are treated as liters and multiplied by 1000, integers shorter than 3 digits are treated as liters, others are assumed to already be cc. Returns null for null/blank or non-numeric input.
     */
    public static Integer parseEngineVolumeCc(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();

        try {
            if (value.contains(".")) {
                return (int) Math.round(Double.parseDouble(value) * 1000);
            }

            int parsed = Integer.parseInt(value);

            if (Integer.toString(Math.abs(parsed)).length() < 3) {
                return parsed * 1000;
            }

            return parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}