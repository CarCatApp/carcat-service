package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.PromoCodeRequest;
import com.carland.carland_service.dto.response.CustomerInformationResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.NotMatchException;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.NotificationRepository;
import com.carland.carland_service.service.PromoCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplApplyPromoTest {

    @Mock Helper helper;
    @Mock CustomerRepository customerRepository;
    @Mock NotificationRepository notificationRepository;
    @InjectMocks UserServiceImpl service;

    @Test
    void matchingCodeIgnoresCaseAndSetsHasPromo() {
        Customer customer = customer();
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer);
        PromoCodeRequest request = new PromoCodeRequest();
        request.setCustomerId(5L);
        request.setPromoCode("  " + PromoCodes.CARCAT_2026.toUpperCase(java.util.Locale.ROOT) + "  ");

        CustomerInformationResponse response = service.applyPromo(request, "USER", "+994501112233", "5", "az");

        assertTrue(response.isHasPromo());
        assertTrue(customer.getHasPromo());
        assertEquals("Leyla", response.getName());
        verify(customerRepository).save(customer);
    }

    @Test
    void inmergeCodeAlsoSetsHasPromo() {
        Customer customer = customer();
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer);
        PromoCodeRequest request = new PromoCodeRequest();
        request.setCustomerId(5L);
        request.setPromoCode("InMerge2026");

        assertTrue(service.applyPromo(request, "USER", "+994501112233", "5", "az").isHasPromo());
        verify(customerRepository).save(customer);
    }

    @Test
    void wrongCodeLeavesHasPromoFalse() {
        Customer customer = customer();
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer);
        PromoCodeRequest request = new PromoCodeRequest();
        request.setCustomerId(5L);
        request.setPromoCode("nope");

        assertThrows(NotMatchException.class,
                () -> service.applyPromo(request, "USER", "+994501112233", "5", "az"));
        assertFalse(Boolean.TRUE.equals(customer.getHasPromo()));
        verify(customerRepository, never()).save(customer);
    }

    @Test
    void customerIdMustMatchTheCaller() {
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer());
        PromoCodeRequest request = new PromoCodeRequest();
        request.setCustomerId(9L);
        request.setPromoCode(PromoCodes.INMERGE_2026);

        assertThrows(NotMatchException.class,
                () -> service.applyPromo(request, "USER", "+994501112233", "5", "az"));
        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blankCodeIsRejected() {
        PromoCodeRequest request = new PromoCodeRequest();
        request.setCustomerId(5L);
        request.setPromoCode("  ");

        assertThrows(MissingFieldException.class,
                () -> service.applyPromo(request, "USER", "+994501112233", "5", "az"));
        verify(customerRepository, never()).findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name());
    }

    private static Customer customer() {
        return Customer.builder()
                .userId(5L)
                .phoneNumber("+994501112233")
                .name("Leyla")
                .status(UserStatus.ACTIVE.name())
                .build();
    }
}
