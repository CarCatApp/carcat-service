package com.carland.carland_service.service;

import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Idempotent customer row for an ACTIVE app USER (same rules as legacy Helper.ensureCustomer).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerEnsureService {

    private final CustomerRepository customerRepository;

    @Transactional
    public void ensure(Long userId, String phoneNumber, String name, String surname, String acceptLanguage) {
        if (userId == null || phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("userId and phoneNumber are required for ensureCustomer");
        }
        String phone = phoneNumber.trim();
        Customer byId = customerRepository.findByUserId(userId);
        Customer byPhone = customerRepository.findByPhoneNumber(phone);

        if ((byId != null && UserStatus.ACTIVE.name().equalsIgnoreCase(byId.getStatus()))
                || (byPhone != null && UserStatus.ACTIVE.name().equalsIgnoreCase(byPhone.getStatus()))) {
            log.info("[customer-ensure] active customer already present | userId={}, phone={}", userId, phone);
            return;
        }
        if (byId != null || byPhone != null) {
            log.warn("[customer-ensure] customer row exists but not ACTIVE | userId={}, phone={}, byIdStatus={}, byPhoneStatus={}",
                    userId, phone,
                    byId != null ? byId.getStatus() : null,
                    byPhone != null ? byPhone.getStatus() : null);
            return;
        }
        customerRepository.save(Customer.builder()
                .userId(userId)
                .phoneNumber(phone)
                .name(name)
                .surname(surname)
                .createdAt(LocalDate.now())
                .status(UserStatus.ACTIVE.name())
                .notificationLanguage(acceptLanguage != null && !acceptLanguage.isBlank() ? acceptLanguage : "az")
                .build());
        log.info("[customer-ensure] customer created | userId={}, phone={}", userId, phone);
    }

    public boolean hasActiveCustomer(Long userId, String phoneNumber) {
        if (userId == null || phoneNumber == null || phoneNumber.isBlank()) {
            return false;
        }
        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(
                userId, phoneNumber.trim(), UserStatus.ACTIVE.name());
        return customer != null;
    }
}
