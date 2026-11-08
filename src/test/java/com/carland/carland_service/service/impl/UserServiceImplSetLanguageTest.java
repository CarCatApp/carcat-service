package com.carland.carland_service.service.impl;

import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.NotMatchException;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplSetLanguageTest {

    @Mock Helper helper;
    @Mock CustomerRepository customerRepository;
    @Mock NotificationRepository notificationRepository;
    @InjectMocks UserServiceImpl service;

    @Test
    void storesAzEnOrRuAndLeavesTheRestOfTheProfile() {
        Customer customer = customer("az");
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer);

        assertEquals("en", service.setNotificationLanguage("en-US", "USER", "+994501112233", "5", "az").getLanguage());
        assertEquals("ru", service.setNotificationLanguage("RU", "USER", "+994501112233", "5", "en").getLanguage());
        assertEquals("az", service.setNotificationLanguage("az-AZ", "USER", "+994501112233", "5", "ru").getLanguage());

        assertEquals("az", customer.getNotificationLanguage());
        assertEquals("Leyla", customer.getName());
        verify(customerRepository, times(3)).save(customer);
    }

    @Test
    void rejectsALanguageOutsideAzEnRu() {
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer("az"));

        assertThrows(NotMatchException.class,
                () -> service.setNotificationLanguage("tr", "USER", "+994501112233", "5", "az"));
        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsABlankLanguage() {
        assertThrows(MissingFieldException.class,
                () -> service.setNotificationLanguage("  ", "USER", "+994501112233", "5", "az"));
        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static Customer customer(String language) {
        return Customer.builder()
                .userId(5L)
                .phoneNumber("+994501112233")
                .name("Leyla")
                .notificationLanguage(language)
                .status(UserStatus.ACTIVE.name())
                .build();
    }
}
