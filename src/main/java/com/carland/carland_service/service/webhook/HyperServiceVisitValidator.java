package com.carland.carland_service.service.webhook;

import com.carland.carland_service.dto.response.hyper.HyperServiceHistoryItemResponse;
import com.carland.carland_service.dto.response.hyper.HyperServiceLineResponse;
import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.exceptions.MissingFieldException;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * tr: Hyper webhook payload'ını doğrulayan yardımcı sınıf (utility); tek ziyaretlik servis geçmişi isteğinin zorunlu alanlarını kontrol eder. Örneklenemez, sadece statik metodlar içerir.
 * en: Utility class validating the Hyper webhook payload; checks the required fields of a single-visit service history request. Non-instantiable, contains only static methods.
 */
public final class HyperServiceVisitValidator {

    private HyperServiceVisitValidator() {
    }

    /**
     * tr: Webhook isteğini doğrular: request, partnerId, vin ve plate zorunludur; serviceHistory tam olarak 1 ziyaret içermelidir. Ziyarette recordId, lastServiceDate, lastServiceMileage ve en az bir servis satırı (her satırda serviceCode ve universalServiceId) zorunludur. Herhangi bir eksikte MissingFieldException fırlatır.
     * en: Validates the webhook request: request, partnerId, vin and plate are required; serviceHistory must contain exactly one visit. The visit must have recordId, lastServiceDate, lastServiceMileage and at least one service line (each with serviceCode and universalServiceId). Throws MissingFieldException for any missing field.
     */
    public static void validateSingleVisit(HyperVehicleByVinResponse request) {
        if (request == null) {
            throw MissingFieldException.required("request body");
        }
        if (request.getPartnerId() == null) {
            throw MissingFieldException.required("partnerId");
        }
        requireText(request.getVin(), "vin");
        requireText(request.getPlate(), "plate");

        List<HyperServiceHistoryItemResponse> history = request.getServiceHistory();
        if (history == null || history.isEmpty()) {
            throw MissingFieldException.required("serviceHistory");
        }
        if (history.size() != 1) {
            throw new MissingFieldException("serviceHistory must contain exactly one visit");
        }

        validateVisit(history.get(0));
    }

    private static void validateVisit(HyperServiceHistoryItemResponse visit) {
        if (visit == null) {
            throw MissingFieldException.required("serviceHistory");
        }
        if (visit.getRecordId() == null) {
            throw MissingFieldException.required("recordId");
        }
        if (visit.getLastServiceDate() == null) {
            throw MissingFieldException.required("lastServiceDate");
        }
        if (visit.getLastServiceMileage() == null) {
            throw MissingFieldException.required("lastServiceMileage");
        }

        List<HyperServiceLineResponse> services = visit.getServices();
        if (services == null || services.isEmpty()) {
            throw MissingFieldException.required("services");
        }

        for (HyperServiceLineResponse line : services) {
            validateServiceLine(line);
        }
    }

    private static void validateServiceLine(HyperServiceLineResponse line) {
        if (line == null) {
            throw MissingFieldException.required("services");
        }
        if (line.getServiceCode() == null) {
            throw MissingFieldException.required("serviceCode");
        }
        requireText(line.getUniversalServiceId(), "universalServiceId");
    }

    private static void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw MissingFieldException.required(fieldName);
        }
    }
}
