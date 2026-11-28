package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminPromoSaveRequest;
import com.carland.carland_service.dto.response.AdminPromoRow;
import com.carland.carland_service.entity.Promo;
import com.carland.carland_service.entity.PromoBranch;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.PromoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPromoServiceTest {

    @Mock PromoRepository promoRepository;
    @Mock BranchRepository branchRepository;

    AdminPromoService service;

    @BeforeEach
    void setUp() {
        service = new AdminPromoService(promoRepository, branchRepository);
    }

    @Test
    void createStoresPercentageAndBranches() {
        when(branchRepository.existsById(4L)).thenReturn(true);
        when(branchRepository.existsById(9L)).thenReturn(true);
        when(promoRepository.findByCodeKey("inmerge2026")).thenReturn(Optional.empty());
        when(promoRepository.save(any())).thenAnswer(invocation -> {
            Promo promo = invocation.getArgument(0);
            promo.setId(3L);
            return promo;
        });

        AdminPromoRow row = service.create(body("percentage", new BigDecimal("20"), List.of(4L, 4L, 9L)));

        assertEquals("InMerge2026", row.getCode());
        assertEquals(new BigDecimal("20.00"), row.getDiscountValue());
        assertEquals(List.of(4L, 9L), row.getBranchIds());
        assertFalse(row.getExpired());
        assertEquals(Promo.ACTIVE, row.getStatus());
    }

    @Test
    void amountKeepsTwoDecimalsAndPercentageAbove100IsRejected() {
        when(branchRepository.existsById(4L)).thenReturn(true);
        when(promoRepository.findByCodeKey("inmerge2026")).thenReturn(Optional.empty());
        when(promoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminPromoRow row = service.create(body("amount", new BigDecimal("129.5"), List.of(4L)));

        assertEquals(new BigDecimal("129.50"), row.getDiscountValue());
        assertThrows(InvalidStatusException.class,
                () -> service.create(body("percentage", new BigDecimal("101"), List.of(4L))));
    }

    @Test
    void emptyBranchListIsRejected() {
        assertThrows(MissingFieldException.class,
                () -> service.create(body("percentage", new BigDecimal("20"), List.of())));
        verify(promoRepository, never()).save(any());
    }

    @Test
    void duplicateCodeIsRejected() {
        when(promoRepository.findByCodeKey("inmerge2026")).thenReturn(Optional.of(Promo.builder().id(1L).build()));

        assertThrows(AlreadyExistsException.class,
                () -> service.create(body("percentage", new BigDecimal("20"), List.of(4L))));
        verify(promoRepository, never()).save(any());
    }

    @Test
    void updateReplacesBranchesAndCanTurnExpiredOff() {
        Promo existing = Promo.builder()
                .id(3L)
                .branches(new ArrayList<>())
                .build();
        existing.getBranches().add(PromoBranch.builder().promo(existing).branchId(4L).build());
        when(promoRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(promoRepository.findByCodeKey("inmerge2026")).thenReturn(Optional.of(existing));
        when(branchRepository.existsById(8L)).thenReturn(true);
        when(promoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AdminPromoSaveRequest edit = body("amount", new BigDecimal("10"), List.of(8L));
        edit.setExpired(false);
        edit.setStatus(Promo.DEACTIVE);

        AdminPromoRow row = service.update(3L, edit);

        ArgumentCaptor<Promo> saved = ArgumentCaptor.forClass(Promo.class);
        verify(promoRepository).save(saved.capture());
        assertEquals(List.of(8L), saved.getValue().getBranches().stream().map(PromoBranch::getBranchId).toList());
        assertEquals(Promo.DEACTIVE, row.getStatus());
        assertFalse(row.getExpired());
        assertTrue(saved.getValue().getBranches().stream().allMatch(link -> link.getPromo() == existing));
    }

    @Test
    void updateKeepsBranchAlreadyLinked() {
        Promo existing = Promo.builder()
                .id(1L)
                .branches(new ArrayList<>())
                .build();
        PromoBranch kept = PromoBranch.builder().id(50L).promo(existing).branchId(16L).build();
        existing.getBranches().add(kept);
        existing.getBranches().add(PromoBranch.builder().id(51L).promo(existing).branchId(4L).build());
        when(promoRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(promoRepository.findByCodeKey("inmerge2026")).thenReturn(Optional.of(existing));
        when(branchRepository.existsById(16L)).thenReturn(true);
        when(branchRepository.existsById(8L)).thenReturn(true);
        when(promoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AdminPromoSaveRequest edit = body("percentage", new BigDecimal("20"), List.of(16L, 8L));
        edit.setStatus(Promo.ACTIVE);

        service.update(1L, edit);

        ArgumentCaptor<Promo> saved = ArgumentCaptor.forClass(Promo.class);
        verify(promoRepository).save(saved.capture());
        List<PromoBranch> links = saved.getValue().getBranches();
        assertEquals(List.of(16L, 8L), links.stream().map(PromoBranch::getBranchId).toList());
        assertSame(kept, links.get(0));
        assertNull(links.get(1).getId());
    }

    private static AdminPromoSaveRequest body(String type, BigDecimal value, List<Long> branchIds) {
        AdminPromoSaveRequest body = new AdminPromoSaveRequest();
        body.setName("Yay");
        body.setDescription("yay endirimi");
        body.setCode(" InMerge2026 ");
        body.setStartDate(LocalDate.of(2026, 1, 1));
        body.setEndDate(LocalDate.of(2026, 12, 31));
        body.setDiscountType(type);
        body.setDiscountValue(value);
        body.setBranchIds(branchIds);
        return body;
    }
}
