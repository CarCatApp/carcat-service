package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.AutoServiceRequest;
import com.carland.carland_service.dto.request.ServiceHistoryRequest;
import com.carland.carland_service.dto.request.ServiceRequest;
import com.carland.carland_service.dto.response.AutoServiceResponse;
import com.carland.carland_service.dto.response.ServiceHistoryResponse;
import com.carland.carland_service.dto.response.ServiceResponse;
import com.carland.carland_service.entity.*;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.*;
import com.carland.carland_service.repository.*;
import com.carland.carland_service.service.AutoServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * tr: Oto servis yönetiminin implementasyonudur: SUPER_ADMIN için servis noktası oluşturma ve
 *     ADMIN için araca servis geçmişi ekleme akışlarını içerir; getService ve addServiceAmount
 *     henüz tamamlanmamıştır.
 * en: Implementation of auto service management: creating a service point for SUPER_ADMIN and
 *     inserting service history for a car as ADMIN; getService and addServiceAmount are not
 *     completed yet.
 */
@Service
@RequiredArgsConstructor
public class AutoServiceServiceImpl implements AutoServiceService {

    private final SuperAdminRepository superAdminRepository;
    private final AutoServiceRepository autoServiceRepository;
    private final AdminRepository adminRepository;
    private final CarRepository carRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final ServiceHistoryRepository serviceHistoryRepository;

    /**
     * tr: SUPER_ADMIN rolüyle yeni bir oto servis oluşturur ve süper admin'e bağlar. Rol SUPER_ADMIN
     *     değilse InvalidStatusException, zorunlu alanlar eksikse MissingFieldException, süper admin
     *     bulunamazsa UserNotFoundException, süper adminin zaten servisi varsa AlreadyExistsException fırlatır.
     * en: Creates a new auto service under the SUPER_ADMIN role and links it to the super admin. Throws
     *     InvalidStatusException when the role is not SUPER_ADMIN, MissingFieldException on missing required
     *     fields, UserNotFoundException when the super admin is not found, and AlreadyExistsException when
     *     the super admin already owns a service.
     */
    @Override
    public AutoServiceResponse createAutoService(AutoServiceRequest autoServiceRequest, String phoneNumber, String role,
                                                 String userIdHeader, String timezone, String acceptLanguage) {
        if (!role.equals(UserRoles.SUPER_ADMIN.name())) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }
        if (autoServiceRequest == null || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        SuperAdmin superAdmin = superAdminRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader),
                phoneNumber, UserStatus.ACTIVE.name());

        if (superAdmin == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        AutoService autoService = autoServiceRepository.findBySuperAdmin(superAdmin);

        if (autoService != null) {
            throw new AlreadyExistsException(MessagesLangValues.AUTO_SERVICE_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        }

        AutoService newAutoService = AutoService.builder()
                .name(autoServiceRequest.getName())
                .address(autoServiceRequest.getAddress())
                .phoneNumber(autoServiceRequest.getPhoneNumber())
                .email(autoServiceRequest.getEmail())
                .superAdmin(superAdmin)
                .build();


        autoServiceRepository.save(newAutoService);
        superAdmin.setAutoService(newAutoService);
        superAdminRepository.save(superAdmin);
        return AutoServiceResponse.builder()
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    /**
     * tr: ADMIN rolüyle, VIN'i verilen araca yeni bir servis geçmişi kaydı ekler ve kaydı döner.
     *     Eksik alanlarda MissingFieldException, rol ADMIN değilse InvalidStatusException, admin
     *     bulunamazsa UserNotFoundException; oto servis, araç veya servis tanımı bulunamazsa
     *     ResourceNotFoundException fırlatır.
     * en: Inserts a new service history record for the car with the given VIN under the ADMIN role and
     *     returns it. Throws MissingFieldException on missing fields, InvalidStatusException when the role
     *     is not ADMIN, UserNotFoundException when the admin is not found, and ResourceNotFoundException
     *     when the auto service, car, or service definition cannot be found.
     */
    @Override
    public ServiceHistoryResponse insertServiceHistory(ServiceHistoryRequest request, String phoneNumber,
                                                       String userIdHeader, String role, String timezone,
                                                       String acceptLanguage) {

        if (request == null || request.getVin() == null || phoneNumber == null || userIdHeader == null || role == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        if (!role.equals(UserRoles.ADMIN.name())) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }

        Admin admin = adminRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader), phoneNumber,
                UserStatus.ACTIVE.name());

        if (admin == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        AutoService autoService = admin.getAutoService();

        if (autoService == null) {
            throw new ResourceNotFoundException(MessagesLangValues.AUTO_SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        Car car = carRepository.findByVin(request.getVin());

        if (car == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        ServiceEntity serviceEntity = serviceEntityRepository.findByServiceName(request.getServiceName());
        if (serviceEntity == null) {
            throw new ResourceNotFoundException(MessagesLangValues.SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        ServiceHistory serviceHistory = ServiceHistory.builder()
                .serviceName(request.getServiceName())
                .actionType(Collections.singletonList(serviceEntity.getActionType()))
                .serviceAmount(request.getServiceAmount())
                .serviceCenter(autoService.getName())
                .serviceCenterId(autoService.getId())
                .doneDate(request.getDoneDate())
                .doneKm(request.getDoneKm())
                .car(car)
                .build();
        serviceHistoryRepository.save(serviceHistory);

        return ServiceHistoryResponse.builder()
                .id(serviceHistory.getId())
                .serviceName(serviceHistory.getServiceName())
                .actionType(serviceHistory.getActionType() == null ? null : String.join(", ", serviceHistory.getActionType()))
                .doneKm(serviceHistory.getDoneKm())
                .doneDate(serviceHistory.getDoneDate())
                .serviceAmount(serviceHistory.getServiceAmount())
                .serviceCenter(serviceHistory.getServiceCenter())
                .serviceCenterId(serviceHistory.getServiceCenterId() != null ? serviceHistory.getServiceCenterId() : 1L)
                .build();
    }

    /**
     * tr: Henüz tamamlanmadı: servis adı yoksa MissingFieldException fırlatır, aksi halde null döner.
     * en: Not completed yet: throws MissingFieldException when the service name is missing, otherwise returns null.
     */
    @Override
    public ServiceResponse getService(ServiceRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {
        if (request.getServiceName() == null ) {
            throw new MissingFieldException(MessagesLangValues.SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
//        AutoService autoService = autoServiceRepository.findById(request.getAutoServiceId()).orElseThrow(
//                () -> new ResourceNotFoundException(MessagesLangValues.AUTO_SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage)));

        return null;
    }

    /**
     * tr: Henüz implemente edilmedi; her zaman null döner.
     * en: Not implemented yet; always returns null.
     */
    @Override
    public ServiceResponse addServiceAmount(ServiceRequest request, String phoneNumber, String userIdHeader, String role, String timezone, String acceptLanguage) {
        return null;
    }

}
