package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.AppointmentResponse;
import com.carland.carland_service.service.AppointmentService;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * tr: AppointmentService sözleşmesinin implementasyonudur. Şu an her iki metot da henüz
 *     implemente edilmemiştir (placeholder) ve null döner.
 * en: Implementation of the AppointmentService contract. Both methods are currently
 *     unimplemented placeholders and return null.
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {
    /**
     * tr: Henüz implemente edilmedi; her zaman null döner.
     * en: Not implemented yet; always returns null.
     */
    @Override
    public List<AppointmentResponse> getBookingListByDate(String date, String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {
        return null;
    }

    /**
     * tr: Henüz implemente edilmedi; her zaman null döner.
     * en: Not implemented yet; always returns null.
     */
    @Override
    public AppointmentResponse setAppointmentFromReception(String role, String phoneNumber, Long rangeId, String userIdHeader, String timezone, String acceptLanguage) {
        return null;
    }
}
