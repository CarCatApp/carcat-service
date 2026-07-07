package com.carland.carland_service.service.impl;

import com.carland.carland_service.feign.NhtsaFeign;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VinService {

    private final NhtsaFeign nhtsaFeign;

    public Map<String, Object> decodeVin(String vin) {
        return nhtsaFeign.decodeVin(vin, "json");
    }

    public Map<String, String> extractFieldsFromVin(String vin) {
        Map<String, Object> response = decodeVin(vin);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) response.get("Results");

        Map<String, String> output = new HashMap<>();

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

                case "Fuel Type - Primary" -> {
                    String engineType = mapEngineType(value);
                    if (engineType != null) {
                        output.put("engineType", engineType);
                    }
                }
            }
        }

        return output;
    }

    private String mapEngineType(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.toLowerCase().trim();

        if (normalized.contains("diesel hybrid")) {
            return "Diesel Hybrid";

        } else if (normalized.contains("plug-in hybrid")
                || normalized.contains("plug in hybrid")
                || normalized.contains("phev")) {

            return "Plug-in Hybrid";

        } else if (normalized.contains("hybrid")) {

            return "Hybrid";

        } else if (normalized.contains("electric")
                || normalized.contains("battery electric")
                || normalized.contains("bev")) {

            return "Electric";

        } else if (normalized.contains("lpg")
                || normalized.contains("cng")
                || normalized.contains("natural gas")) {

            return "Gas (LPG / CNG)";

        } else if (normalized.contains("diesel")) {

            return "Diesel";

        } else if (normalized.contains("gasoline")
                || normalized.contains("petrol")) {

            return "Petrol (Gasoline)";
        }

        return null;
    }

    /**
     * NHTSA "Displacement (L)" is usually liters (1.5, 2.0).
     * Sometimes whole liters without decimals (5 → 5L).
     * Values already in cc/ml are typically 3–4 digits (890, 1200, 3500).
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