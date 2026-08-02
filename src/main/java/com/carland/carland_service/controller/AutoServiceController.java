package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AutoServiceRequest;
import com.carland.carland_service.dto.request.ServiceHistoryRequest;
import com.carland.carland_service.dto.request.ServiceRequest;
import com.carland.carland_service.dto.response.AutoServiceResponse;
import com.carland.carland_service.dto.response.ServiceHistoryResponse;
import com.carland.carland_service.dto.response.ServiceResponse;
import com.carland.carland_service.service.AutoServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * tr: Oto servis REST controller'ı; servis oluşturma, servis geçmişi ekleme, servis sorgulama ve servise tutar ekleme uçlarını sunar.
 * en: REST controller for auto services; exposes endpoints to create a service, insert service history, fetch a service, and add an amount to a service.
 */
@RestController
@RequestMapping("/api/v1/auto-service")
@RequiredArgsConstructor
@Slf4j
public class AutoServiceController {
    private final AutoServiceService autoServiceService;

    /**
     * tr: Gövdedeki AutoServiceRequest ile yeni bir oto servis kaydı oluşturur; phoneNumber, role, X-User-Id, timezone ve Accept-Language header'larını servise iletir ve oluşturulan kaydı döner.
     * en: Creates a new auto service record from the AutoServiceRequest body; forwards the phoneNumber, role, X-User-Id, timezone, and Accept-Language headers to the service layer and returns the created record.
     */
    @PostMapping("/create")
    public AutoServiceResponse createAutoService(@RequestBody AutoServiceRequest autoServiceRequest,
                                                 @RequestHeader("Authorization") String token,
                                                 @RequestHeader("phoneNumber") String phoneNumber,
                                                 @RequestHeader("role") String role,
                                                 @RequestHeader("X-User-Id") String userIdHeader,
                                                 @RequestHeader("X-Client-Timezone") String timezone,
                                                 @RequestHeader("Accept-Language") String acceptLanguage) {


        return autoServiceService.createAutoService(autoServiceRequest, phoneNumber, role, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Gövdedeki ServiceHistoryRequest ile bir araca servis geçmişi kaydı ekler; kullanıcı/rol bilgileri header'lardan alınır ve eklenen geçmiş kaydı döner.
     * en: Inserts a service history record for a car from the ServiceHistoryRequest body; user/role info comes from headers, returns the inserted history record.
     */
    @PostMapping("/insert/service/history")

    public ServiceHistoryResponse insertServiceHistory(@RequestHeader("Authorization") String token,
                                                       @RequestBody ServiceHistoryRequest request,
                                                       @RequestHeader("phoneNumber") String phoneNumber,
                                                       @RequestHeader("X-User-Id") String userIdHeader,
                                                       @RequestHeader("role") String role,
                                                       @RequestHeader("X-Client-Timezone") String timezone,
                                                       @RequestHeader("Accept-Language") String acceptLanguage) {
        return autoServiceService.insertServiceHistory(request, phoneNumber, userIdHeader, role, timezone, acceptLanguage);
    }

    /**
     * tr: Gövdedeki ServiceRequest kriterlerine göre servis bilgisini getirir; phoneNumber ve X-User-Id header'ları ile çağıran kullanıcı bağlamı servise iletilir.
     * en: Fetches service information based on the ServiceRequest body criteria; the caller context is passed to the service layer via the phoneNumber and X-User-Id headers.
     */
    @GetMapping("/get/service")
    public ServiceResponse getService(@RequestHeader("Authorization") String token,
                                      @RequestBody ServiceRequest request,
                                      @RequestHeader("phoneNumber") String phoneNumber,
                                      @RequestHeader("X-User-Id") String userIdHeader,
                                      @RequestHeader("X-Client-Timezone") String timezone,
                                      @RequestHeader("Accept-Language") String acceptLanguage) {
        return autoServiceService.getService(request, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Gövdedeki ServiceRequest ile mevcut bir servise tutar (amount) ekler; rol ve kullanıcı bilgileri header'lardan alınır ve güncellenen servis döner.
     * en: Adds an amount to an existing service using the ServiceRequest body; role and user info come from headers, returns the updated service.
     */
    @PostMapping("/add/service/amount")
    public ServiceResponse addServiceAmount(@RequestHeader("Authorization") String token,
                                            @RequestBody ServiceRequest request,
                                            @RequestHeader("phoneNumber") String phoneNumber,
                                            @RequestHeader("X-User-Id") String userIdHeader,
                                            @RequestHeader("role") String role,
                                            @RequestHeader("X-Client-Timezone") String timezone,
                                            @RequestHeader("Accept-Language") String acceptLanguage) {
        return autoServiceService.addServiceAmount(request, phoneNumber, userIdHeader, role, timezone, acceptLanguage);
    }
}
