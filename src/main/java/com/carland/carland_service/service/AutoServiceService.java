package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AutoServiceRequest;
import com.carland.carland_service.dto.request.ServiceHistoryRequest;
import com.carland.carland_service.dto.request.ServiceRequest;
import com.carland.carland_service.dto.response.AutoServiceResponse;
import com.carland.carland_service.dto.response.ServiceHistoryResponse;
import com.carland.carland_service.dto.response.ServiceResponse;

/**
 * tr: Oto servis (servis noktası) yönetimi sözleşmesidir: servis oluşturma, servis geçmişi ekleme,
 *     servis bilgisi sorgulama ve servis tutarı ekleme işlemlerini tanımlar.
 * en: Contract for auto service (service point) management: creating a service, inserting service history,
 *     querying service info, and adding a service amount.
 */
public interface AutoServiceService {
    /**
     * tr: Yeni bir oto servis kaydı oluşturur ve oluşturulan servisi döner.
     * en: Creates a new auto service record and returns the created service.
     */
    AutoServiceResponse createAutoService(AutoServiceRequest autoServiceRequest, String phoneNumber, String role,String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Bir araca ait servis geçmişi kaydı ekler ve eklenen geçmişi döner.
     * en: Inserts a service history record for a car and returns the inserted history.
     */
    ServiceHistoryResponse insertServiceHistory(ServiceHistoryRequest request, String phoneNumber, String userIdHeader, String role, String timezone, String acceptLanguage);

    /**
     * tr: İstenen servis kaydının detaylarını döner.
     * en: Returns the details of the requested service record.
     */
    ServiceResponse getService(ServiceRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Var olan bir servis kaydına tutar (amount) bilgisi ekler ve güncel servisi döner.
     * en: Adds an amount to an existing service record and returns the updated service.
     */
    ServiceResponse addServiceAmount(ServiceRequest request, String phoneNumber, String userIdHeader, String role, String timezone, String acceptLanguage);

}
