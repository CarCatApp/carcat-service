package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.MaintenanceTemplateRequest;
import com.carland.carland_service.dto.request.ServiceRequest;
import com.carland.carland_service.dto.response.MaintenanceTemplateResponse;
import com.carland.carland_service.dto.response.ServiceResponse;
import com.carland.carland_service.entity.Brand;
import com.carland.carland_service.entity.EngineType;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BrandRepository;
import com.carland.carland_service.repository.MaintenanceTemplateRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.carland.carland_service.service.MaintenanceTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * tr: Araç bakım şablonlarını (MaintenanceTemplate) yöneten servis; şablon listeleme ve şablona servis kalemi ekleme işlemlerini yapar. Şablon oluşturma metodu şu an devre dışıdır (gövdesi yorum satırında).
 * en: Service managing vehicle maintenance templates (MaintenanceTemplate); handles listing templates and adding service items to a template. The template creation method is currently disabled (body commented out).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceTemplateServiceImpl implements MaintenanceTemplateService {


    @Value("${super.admin.phone}")
    private String superAdminPhoneNumber;


    private final MaintenanceTemplateRepository maintenanceTemplateRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final BrandRepository brandRepository;

    /**
     * tr: Yeni bakım şablonu oluşturma metodu; asıl mantık (rol/telefon doğrulama, mevcut şablon kontrolü, kayıt) yorum satırına alınmıştır, şu an her zaman null döner.
     * en: Method for creating a new maintenance template; the actual logic (role/phone validation, duplicate check, persistence) is commented out, so it currently always returns null.
     */
    @Override
    public MaintenanceTemplateResponse createMaintenanceTemplate(MaintenanceTemplateRequest request,
                                                                 String role, String phoneNumber, String userIdHeader,
                                                                 String timezone, String acceptLanguage) {
//        if (!phoneNumber.equals(superAdminPhoneNumber) || !role.equals(UserRoles.BOSS.name()) || !userIdHeader.equals("1")) {
//            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
//        }
//
//        if (request.getEngineType() == null ) {
//            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
//        }
//
//
//        MaintenanceTemplate existingTemplate = maintenanceTemplateRepository.
//                findByBrandAndModelAndYearAndEngineTypeAndTransmissionType(
//                        request.getBrand(), request.getModel(), request.getYear(), request.getEngineType(), request.getTransmissionType());
//        if (existingTemplate != null) {
//            throw new AlreadyExistsException(MessagesLangValues.TEMPLATE_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
//        }
//
//        MaintenanceTemplate newTemplate = MaintenanceTemplate.builder()
//                .engineType(request.getEngineType())
//                .transmissionType(request.getTransmissionType())
//                .build();
//
//        maintenanceTemplateRepository.save(newTemplate);
//
//        boolean exists = brandRepository.existsByBrandName(newTemplate.getBrand());
//
//        if (exists) {
//            log.info("brand found, not create new brand");
//        } else {
//            Brand brand = Brand.builder()
//                    .brandName(newTemplate.getBrand())
//                    .status(UserStatus.ACTIVE.name())
//                    .build();
//
//            brandRepository.save(brand);
//        }

        return null;
    }

    /**
     * tr: Tüm bakım şablonlarını getirir ve dil bazlı MaintenanceTemplateResponse listesine çevirir. Hiç şablon yoksa ResourceNotFoundException fırlatır.
     * en: Fetches all maintenance templates and converts them to a language-aware list of MaintenanceTemplateResponse. Throws ResourceNotFoundException if no templates exist.
     */
    @Override
    public List<MaintenanceTemplateResponse> getMaintenanceTemplateList(String phoneNumber, String userIdHeader,
                                                                        String timezone, String acceptLanguage) {

        List<MaintenanceTemplate> templates = maintenanceTemplateRepository.findAll();

        if (templates.isEmpty()) {
            throw new ResourceNotFoundException(MessagesLangValues.TEMPLATE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        return templates.stream().map(template -> convert(template, acceptLanguage)).toList();
    }

    /**
     * tr: Verilen şablona yeni bir servis kalemi (ServiceEntity) ekler; zorunlu alanlar eksikse MissingFieldException (km/ay aralığından en az biri şarttır), şablon bulunamazsa ResourceNotFoundException, aynı servis zaten varsa AlreadyExistsException fırlatır. Güncellenmiş şablonu servis listesiyle birlikte döner.
     * en: Adds a new service item (ServiceEntity) to the given template; throws MissingFieldException if required fields are missing (at least one of km/month interval is required), ResourceNotFoundException if the template is not found, and AlreadyExistsException if an identical service already exists. Returns the updated template with its service list.
     */
    @Override
    public MaintenanceTemplateResponse addServiceToTemplate(Long templateId, ServiceRequest request, String phoneNumber,
                                                            String userIdHeader, String role, String timezone, String acceptLanguage) {

        if (templateId == null || request == null || request.getServiceName() == null || request.getActionType() == null || role == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        if (request.getIntervalKm() == null && request.getIntervalMonth() == null) {
            throw new MissingFieldException("zaman ve km intervallarinin en azi biri daxil edilmelidir");
        }
//        if (!phoneNumber.equals(superAdminPhoneNumber) || !role.equals(UserRoles.BOSS.name()) || !userIdHeader.equals("1")) {
//            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
//        }

        MaintenanceTemplate template = maintenanceTemplateRepository.findById(templateId).orElseThrow(
                () -> new ResourceNotFoundException(MessagesLangValues.TEMPLATE_NOT_FOUND.getMessageByLang(acceptLanguage)));

        ServiceEntity service = serviceEntityRepository.findByServiceNameAndActionTypeAndIntervalKmAndIntervalMonthAndMaintenanceTemplate(
                request.getServiceName(), request.getActionType(), request.getIntervalKm(), request.getIntervalMonth(), template
        );

        if (service != null) {
            throw new AlreadyExistsException(MessagesLangValues.SERVICE_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        }

        ServiceEntity newService = ServiceEntity.builder()
                .serviceName(request.getServiceName())
                .actionType(request.getActionType())
                .intervalMonth(request.getIntervalMonth())
                .intervalKm(request.getIntervalKm())
                .maintenanceTemplate(template)
                .nameAz(request.getNameAz())
                .nameEn(request.getNameEn())
                .nameRu(request.getNameRu())
                .build();

        template.getServices().add(newService);

        serviceEntityRepository.save(newService);
        maintenanceTemplateRepository.save(template);

        List<ServiceResponse> responses = template.getServices().stream().map(s-> convert(s,acceptLanguage)).toList();
        MaintenanceTemplateResponse response = convert(template, acceptLanguage);
        response.setServiceResponseList(responses);

        return response;
    }

    private MaintenanceTemplateResponse convert(MaintenanceTemplate template, String acceptLanguage) {

        EngineType engineType = template.getEngineType();
        return MaintenanceTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .engineType(engineType.getEngineType())
                .engineTypeId(engineType.getEngineTypeId())
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .serviceResponseList(template.getServices().stream().map(service -> convert(service, acceptLanguage)).toList())
                .build();
    }

    private ServiceResponse convert(ServiceEntity service, String acceptLanguage) {



        String serviceNameTranslated = switch (acceptLanguage) {
            case "az" -> service.getNameAz();
            case "ru" -> service.getNameRu();
            default -> service.getNameEn();
        };

        return ServiceResponse.builder()
                .id(service.getId())
                .serviceName(serviceNameTranslated)
                .actionType(service.getActionType())
                .nameAz(service.getNameAz())
                .nameEn(service.getNameEn())
                .nameRu(service.getNameRu())
                .intervalMonth(service.getIntervalMonth())
                .intervalKm(service.getIntervalKm())
                .important(service.isImportant())
                .build();
    }
}
