package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.PromoPreviewResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Promo;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.PromoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromoPreviewServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock PromoRepository promoRepository;
    @InjectMocks PromoPreviewService service;

    @Test
    void usableCodeForAPhoneWithoutPromo() {
        when(customerRepository.findByPhoneNumber("+994501112233")).thenReturn(null);
        when(promoRepository.findByCodeKey("carcat2026")).thenReturn(Optional.of(usable()));

        PromoPreviewResponse response = service.preview("0501112233", "  CarCat2026  ");

        assertFalse(response.isHasPromo());
        assertFalse(response.isSameCode());
        assertEquals(PromoPreviewService.USABLE, response.getCodeState());
    }

    @Test
    void sameStoredCodeIsReported() {
        Customer customer = Customer.builder()
                .phoneNumber("+994501112233")
                .hasPromo(true)
                .promoCode("CarCat2026")
                .build();
        when(customerRepository.findByPhoneNumber("+994501112233")).thenReturn(customer);
        when(promoRepository.findByCodeKey("carcat2026")).thenReturn(Optional.of(usable()));

        PromoPreviewResponse response = service.preview("+994501112233", "carcat2026");

        assertTrue(response.isHasPromo());
        assertTrue(response.isSameCode());
    }

    @Test
    void aDifferentStoredCodeIsNotTheSame() {
        Customer customer = Customer.builder()
                .phoneNumber("+994501112233")
                .hasPromo(true)
                .promoId(3L)
                .promoCode("inmerge2026")
                .build();
        when(customerRepository.findByPhoneNumber("+994501112233")).thenReturn(customer);
        when(promoRepository.findByCodeKey("carcat2026")).thenReturn(Optional.of(usable()));

        PromoPreviewResponse response = service.preview("+994501112233", "carcat2026");

        assertTrue(response.isHasPromo());
        assertFalse(response.isSameCode());
    }

    @Test
    void missingDeactiveExpiredAndNotStarted() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Baku"));
        assertEquals(PromoPreviewService.NOT_FOUND, PromoPreviewService.codeState(null, today));

        Promo off = usable();
        off.setStatus(Promo.DEACTIVE);
        assertEquals(PromoPreviewService.DEACTIVATED, PromoPreviewService.codeState(off, today));

        Promo flagged = usable();
        flagged.setExpired(true);
        assertEquals(PromoPreviewService.EXPIRED, PromoPreviewService.codeState(flagged, today));

        Promo ended = usable();
        ended.setEndDate(today.minusDays(1));
        assertEquals(PromoPreviewService.EXPIRED, PromoPreviewService.codeState(ended, today));

        Promo later = usable();
        later.setStartDate(today.plusDays(1));
        assertEquals(PromoPreviewService.NOT_STARTED, PromoPreviewService.codeState(later, today));
    }

    @Test
    void blankInputIsRejected() {
        assertThrows(MissingFieldException.class, () -> service.preview("+994501112233", "  "));
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
