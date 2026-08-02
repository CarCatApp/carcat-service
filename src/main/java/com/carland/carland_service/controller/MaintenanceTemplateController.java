package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.MaintenanceTemplateRequest;
import com.carland.carland_service.dto.request.ServiceRequest;
import com.carland.carland_service.dto.response.MaintenanceTemplateResponse;
import com.carland.carland_service.service.MaintenanceTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * tr: Bakım şablonu REST controller'ı; şablon oluşturma, şablon listeleme ve şablona servis ekleme uçlarını sunar.
 * en: REST controller for maintenance templates; exposes endpoints to create a template, list templates, and add a service to a template.
 */
@RestController
@RequestMapping("/api/v1/template")
@RequiredArgsConstructor

public class MaintenanceTemplateController {

    private final MaintenanceTemplateService maintenanceTemplateService;


    /**
     * tr: Gövdedeki MaintenanceTemplateRequest ile yeni bir bakım şablonu oluşturur; role, phoneNumber ve X-User-Id header'ları servise iletilir ve oluşturulan şablon döner.
     * en: Creates a new maintenance template from the MaintenanceTemplateRequest body; the role, phoneNumber, and X-User-Id headers are forwarded to the service layer, returns the created template.
     */
    @PostMapping("/create")
    public MaintenanceTemplateResponse createMaintenanceTemplate(@RequestBody MaintenanceTemplateRequest maintenanceTemplateRequest,
                                                                 @RequestHeader("Authorization") String token,
                                                                 @RequestHeader("role") String role,
                                                                 @RequestHeader("phoneNumber") String phoneNumber,
                                                                 @RequestHeader("X-User-Id") String userIdHeader,
                                                                 @RequestHeader("X-Client-Timezone") String timezone,
                                                                 @RequestHeader("Accept-Language") String acceptLanguage) {
        return maintenanceTemplateService.createMaintenanceTemplate(maintenanceTemplateRequest, role, phoneNumber,
                userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Çağıran kullanıcı için mevcut bakım şablonlarının listesini döner.
     * en: Returns the list of maintenance templates available to the calling user.
     */
    @GetMapping("/list")
    public List<MaintenanceTemplateResponse> getMaintenanceTemplateList(@RequestHeader("Authorization") String token,
                                                                        @RequestHeader("phoneNumber") String phoneNumber,
                                                                        @RequestHeader("X-User-Id") String userIdHeader,
                                                                        @RequestHeader("X-Client-Timezone") String timezone,
                                                                        @RequestHeader("Accept-Language") String acceptLanguage) {
        return maintenanceTemplateService.getMaintenanceTemplateList(phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Verilen templateId'ye ait bakım şablonuna, gövdedeki ServiceRequest ile yeni bir servis kalemi ekler ve güncellenmiş şablonu döner.
     * en: Adds a new service item from the ServiceRequest body to the maintenance template identified by templateId and returns the updated template.
     */
    @PostMapping("/add/service")
    public MaintenanceTemplateResponse addServiceToTemplate(@RequestHeader("Authorization") String token,
                                                            @RequestParam Long templateId,
                                                            @RequestBody ServiceRequest request,
                                                            @RequestHeader("phoneNumber") String phoneNumber,
                                                            @RequestHeader("X-User-Id") String userIdHeader,
                                                            @RequestHeader("role") String role,
                                                            @RequestHeader("X-Client-Timezone") String timezone,
                                                            @RequestHeader("Accept-Language") String acceptLanguage){
        return maintenanceTemplateService.addServiceToTemplate(templateId, request, phoneNumber, userIdHeader, role, timezone, acceptLanguage);

    }


}
