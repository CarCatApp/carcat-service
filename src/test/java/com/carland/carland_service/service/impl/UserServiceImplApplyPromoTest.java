package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.PromoCodeRequest;
import com.carland.carland_service.dto.response.CustomerInformationResponse;
import com.carland.carland_service.dto.response.HasPromoResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Promo;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.NotMatchException;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.NotificationRepository;
import com.carland.carland_service.repository.PromoBranchRepository;
import com.carland.carland_service.repository.PromoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

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
    @Mock PromoRepository promoRepository;
    @Mock PromoBranchRepository promoBranchRepository;
    @InjectMocks UserServiceImpl service;

    @Test
    void usableCodeIgnoresCaseAndStoresIt() {
        Customer customer = customer();
        Promo promo = usable();
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer);
        when(promoRepository.findByCodeKey("carcat2026")).thenReturn(Optional.of(promo));

        CustomerInformationResponse response = service.applyPromo(body(5L, "  CarCat2026  "),
                "USER", "+994501112233", "5", "az");

        assertTrue(response.isHasPromo());
        assertEquals("carcat2026", response.getPromoCode());
        assertEquals(8L, customer.getPromoId());
        verify(customerRepository).save(customer);
    }

    @Test
    void expiredOrInactiveCodeIsRejected() {
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer());
        Promo expired = usable();
        expired.setExpired(true);
        when(promoRepository.findByCodeKey("carcat2026")).thenReturn(Optional.of(expired));

        assertThrows(NotMatchException.class,
                () -> service.applyPromo(body(5L, "carcat2026"), "USER", "+994501112233", "5", "az"));
        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void aSecondDifferentCodeIsRejected() {
        Customer customer = customer();
        customer.setHasPromo(true);
        customer.setPromoId(8L);
        customer.setPromoCode("carcat2026");
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer);
        Promo other = usable();
        other.setId(9L);
        other.setCode("inmerge2026");
        when(promoRepository.findByCodeKey("inmerge2026")).thenReturn(Optional.of(other));

        assertThrows(NotMatchException.class,
                () -> service.applyPromo(body(5L, "inmerge2026"), "USER", "+994501112233", "5", "az"));
        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any());
        assertEquals(8L, customer.getPromoId());
    }

    @Test
    void theSameCodeAgainDoesNotWrite() {
        Customer customer = customer();
        customer.setHasPromo(true);
        customer.setPromoId(8L);
        customer.setPromoCode("carcat2026");
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer);
        when(promoRepository.findByCodeKey("carcat2026")).thenReturn(Optional.of(usable()));

        CustomerInformationResponse response = service.applyPromo(body(5L, "carcat2026"),
                "USER", "+994501112233", "5", "az");

        assertEquals("carcat2026", response.getPromoCode());
        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void customerIdMustMatchTheCaller() {
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name()))
                .thenReturn(customer());

        assertThrows(NotMatchException.class,
                () -> service.applyPromo(body(9L, "carcat2026"), "USER", "+994501112233", "5", "az"));
        verify(promoRepository, never()).findByCodeKey(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void hasPromoIsFalseWhenTheCustomerIsMissingOrUnset() {
        when(customerRepository.findByUserId(5L)).thenReturn(null);
        HasPromoResponse missing = service.hasPromo(5L);
        assertFalse(missing.isHasPromo());
        assertEquals(null, missing.getPromo());

        when(customerRepository.findByUserId(5L)).thenReturn(customer());
        assertFalse(service.hasPromo(5L).isHasPromo());

        Customer redeemed = customer();
        redeemed.setHasPromo(true);
        redeemed.setPromoId(8L);
        redeemed.setPromoCode("carcat2026");
        Promo promo = usable();
        promo.setName("Yay");
        promo.setDescription("yay endirimi");
        promo.setDiscountType(Promo.PERCENTAGE);
        promo.setDiscountValue(new BigDecimal("20.00"));
        promo.setBannerId(4L);
        when(customerRepository.findByUserId(5L)).thenReturn(redeemed);
        when(promoRepository.findById(8L)).thenReturn(Optional.of(promo));

        HasPromoResponse response = service.hasPromo(5L);
        assertTrue(response.isHasPromo());
        assertEquals(8L, response.getPromo().getId());
        assertEquals("carcat2026", response.getPromo().getCode());
        assertEquals("Yay", response.getPromo().getName());
        assertEquals(Promo.PERCENTAGE, response.getPromo().getDiscountType());
        assertEquals(4L, response.getPromo().getBannerId());
    }

    @Test
    void supportPromoIsTrueOnlyForAListedBranch() {
        Customer customer = customer();
        customer.setPromoId(8L);
        when(customerRepository.findByUserId(5L)).thenReturn(customer);
        when(promoBranchRepository.existsByPromo_IdAndBranchId(8L, 4L)).thenReturn(true);
        when(promoBranchRepository.existsByPromo_IdAndBranchId(8L, 9L)).thenReturn(false);

        assertTrue(service.supportPromo(5L, 4L).isSupportPromo());
        assertFalse(service.supportPromo(5L, 9L).isSupportPromo());
    }

    @Test
    void supportPromoIsFalseWhenTheUserHasNoPromo() {
        when(customerRepository.findByUserId(5L)).thenReturn(customer());

        assertFalse(service.supportPromo(5L, 4L).isSupportPromo());
        verify(promoBranchRepository, never()).existsByPromo_IdAndBranchId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blankCodeIsRejected() {
        assertThrows(MissingFieldException.class,
                () -> service.applyPromo(body(5L, "  "), "USER", "+994501112233", "5", "az"));
        verify(customerRepository, never()).findByUserIdAndPhoneNumberAndStatus(5L, "+994501112233", UserStatus.ACTIVE.name());
    }

    private static PromoCodeRequest body(Long customerId, String code) {
        PromoCodeRequest request = new PromoCodeRequest();
        request.setCustomerId(customerId);
        request.setPromoCode(code);
        return request;
    }

    private static Customer customer() {
        return Customer.builder()
                .userId(5L)
                .phoneNumber("+994501112233")
                .name("Leyla")
                .status(UserStatus.ACTIVE.name())
                .build();
    }

    private static Promo usable() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Baku"));
        return Promo.builder()
                .id(8L)
                .code("carcat2026")
                .codeKey("carcat2026")
                .status(Promo.ACTIVE)
                .expired(false)
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(1))
                .build();
    }
}
