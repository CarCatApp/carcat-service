package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.ServicedBranchesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * tr: Rozet ekranı. Şube sırası {@code cars.serviced_branch_ids}. Kolon null ise ziyaretlerden bir kez okunur, yazılmaz.
 *     Redis yok.
 * en: Badge screen. Branch order is {@code cars.serviced_branch_ids}. A null column is read from visits and not saved.
 *     No Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicedBranchesService {

    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final VisitRepository visitRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public ServicedBranchesResponse list(Long carId, String userIdHeader, String acceptLanguage) {
        String lang = acceptLanguage == null || acceptLanguage.isBlank() ? "az" : acceptLanguage;
        if (carId == null || userIdHeader == null || userIdHeader.isBlank()
                || acceptLanguage == null || acceptLanguage.isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_FIELDS.getMessageByLang(lang));
        }
        long userId;
        try {
            userId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(lang));
        }
        Customer customer = customerRepository.findByUserId(userId);
        if (customer == null || !UserStatus.ACTIVE.name().equals(customer.getStatus())) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(lang));
        }
        Car car = carRepository.findByCarIdAndCustomer(carId, customer);
        if (car == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(lang));
        }

        List<String> stored = car.getServicedBranchIds();
        List<Visit> visits;
        List<Long> ordered;
        if (stored == null) {
            visits = visitRepository.findByCar_CarIdOrderByLastServiceDateDescIdDesc(carId);
            ordered = idsOf(ServicedBadgeIds.branchIds(visits));
        } else {
            ordered = idsOf(stored);
            visits = ordered.isEmpty()
                    ? List.of()
                    : visitRepository.findByCar_CarIdOrderByLastServiceDateDescIdDesc(carId);
        }
        if (ordered.isEmpty()) {
            return ServicedBranchesResponse.builder().branches(List.of()).build();
        }
        Map<Long, Branch> byId = new HashMap<>();
        for (Branch branch : branchRepository.findAllById(ordered)) {
            byId.put(branch.getId(), branch);
        }
        var cards = ServicedBranchList.cards(ordered, visits, byId, lang);
        log.info("serviced branches | carId={} count={}", carId, cards.size());
        return ServicedBranchesResponse.builder().branches(cards).build();
    }

    private static List<Long> idsOf(List<String> raw) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (raw == null) {
            return List.of();
        }
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                ids.add(Long.valueOf(value.trim()));
            } catch (NumberFormatException ex) {
                continue;
            }
        }
        return new ArrayList<>(ids);
    }
}
