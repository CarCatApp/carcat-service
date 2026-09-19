package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.ServiceHistoryRequest;
import com.carland.carland_service.dto.request.ServiceRequest;
import com.carland.carland_service.dto.response.ServiceHistoryResponse;
import com.carland.carland_service.dto.response.ServiceResponse;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.entity.ServiceHistory;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.carland.carland_service.repository.ServiceHistoryRepository;
import com.carland.carland_service.service.AutoServiceService;
import com.carland.carland_service.service.BookingStaffAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AutoServiceServiceImpl implements AutoServiceService {

    private final BookingStaffAccess bookingStaffAccess;
    private final CarRepository carRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final ServiceHistoryRepository serviceHistoryRepository;

    @Override
    public ServiceHistoryResponse insertServiceHistory(ServiceHistoryRequest request, String phoneNumber,
                                                       String userIdHeader, String role, String timezone,
                                                       String acceptLanguage) {
        if (request == null || request.getVin() == null || phoneNumber == null || userIdHeader == null || role == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        BookingStaff staff = bookingStaffAccess.requireActive(Long.valueOf(userIdHeader), acceptLanguage);
        String centerName = staff.getBranch() != null ? staff.getBranch().getName() : staff.getPartner().getName();
        Long centerId = staff.getPartner().getId();

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
                .serviceCenter(centerName)
                .serviceCenterId(centerId)
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

    @Override
    public ServiceResponse getService(ServiceRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {
        if (request.getServiceName() == null) {
            throw new MissingFieldException(MessagesLangValues.SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        return null;
    }

    @Override
    public ServiceResponse addServiceAmount(ServiceRequest request, String phoneNumber, String userIdHeader, String role, String timezone, String acceptLanguage) {
        return null;
    }
}
