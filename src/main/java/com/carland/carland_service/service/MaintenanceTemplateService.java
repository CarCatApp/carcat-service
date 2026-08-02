package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.MaintenanceTemplateRequest;
import com.carland.carland_service.dto.request.ServiceRequest;
import com.carland.carland_service.dto.response.MaintenanceTemplateResponse;

import java.util.List;

/**
 * tr: Bakım şablonlarının (maintenance template) oluşturulması, listelenmesi ve şablona servis eklenmesi için sözleşmedir.
 * en: Contract for creating and listing maintenance templates and adding services to a template.
 */
public interface MaintenanceTemplateService {
    /**
     * tr: Yeni bir bakım şablonu oluşturur ve oluşturulan şablonu döner.
     * en: Creates a new maintenance template and returns the created template.
     */
    MaintenanceTemplateResponse createMaintenanceTemplate(MaintenanceTemplateRequest maintenanceTemplateRequest,
                                                          String role, String phoneNumber, String userIdHeader,
                                                          String timezone, String acceptLanguage);

    /**
     * tr: Tanımlı bakım şablonlarının listesini döner.
     * en: Returns the list of defined maintenance templates.
     */
    List<MaintenanceTemplateResponse> getMaintenanceTemplateList(String phoneNumber, String userIdHeader,
                                                                 String timezone, String acceptLanguage);


    /**
     * tr: Var olan bir şablona yeni servis kalemi ekler ve güncel şablonu döner.
     * en: Adds a new service item to an existing template and returns the updated template.
     */
    MaintenanceTemplateResponse addServiceToTemplate(Long templateId, ServiceRequest request, String phoneNumber,
                                                     String userIdHeader, String roel, String timezone, String acceptLanguage);
}
