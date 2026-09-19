package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.ServiceHistoryRequest;
import com.carland.carland_service.dto.request.ServiceRequest;
import com.carland.carland_service.dto.response.ServiceHistoryResponse;
import com.carland.carland_service.dto.response.ServiceResponse;

/**
 * tr: Staff'ın araca servis geçmişi yazması (eski auto-service create yok).
 * en: Staff writes car service history (legacy auto-service create is gone).
 */
public interface AutoServiceService {

    ServiceHistoryResponse insertServiceHistory(ServiceHistoryRequest request, String phoneNumber, String userIdHeader, String role, String timezone, String acceptLanguage);

    ServiceResponse getService(ServiceRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);

    ServiceResponse addServiceAmount(ServiceRequest request, String phoneNumber, String userIdHeader, String role, String timezone, String acceptLanguage);
}
