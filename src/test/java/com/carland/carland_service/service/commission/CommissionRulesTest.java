package com.carland.carland_service.service.commission;

import com.carland.carland_service.entity.CommissionRate;
import com.carland.carland_service.entity.Visit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommissionRulesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 15);

    @Test
    void earlierHyperDayIsOld() {
        CommissionDecision decision = CommissionStatusRules.decide(DAY,
                HyperVinSnapshot.found(List.of(LocalDate.of(2026, 10, 14)), "1 2026-10-14"));
        assertEquals(CommissionStatus.OLD, decision.status());
        assertEquals(CommissionStatusRules.FOUND, decision.outcome());
        assertEquals(LocalDate.of(2026, 10, 14), decision.earliestServiceDate());
    }

    @Test
    void sameDayAndLaterAndMissingAreNew() {
        assertEquals(CommissionStatus.NEW, CommissionStatusRules.decide(DAY,
                HyperVinSnapshot.found(List.of(DAY), "same")).status());
        assertEquals(CommissionStatus.NEW, CommissionStatusRules.decide(DAY,
                HyperVinSnapshot.found(List.of(DAY.plusDays(1)), "later")).status());
        assertEquals(CommissionStatusRules.NOT_FOUND, CommissionStatusRules.decide(DAY,
                HyperVinSnapshot.notFound("empty")).outcome());
        assertEquals(CommissionStatusRules.TIMEOUT, CommissionStatusRules.decide(DAY,
                HyperVinSnapshot.failed("timeout")).outcome());
        assertEquals(CommissionStatus.NEW, CommissionStatusRules.decide(DAY,
                HyperVinSnapshot.found(List.of(), "no-date")).status());
    }

    @Test
    void ticketExampleAmounts() {
        assertEquals(new BigDecimal("15.00"), CommissionMoney.commission(new BigDecimal("250.00"), new BigDecimal("6")));
        assertEquals(new BigDecimal("3.60"), CommissionMoney.commission(new BigDecimal("180.00"), new BigDecimal("2")));
        assertEquals(new BigDecimal("5.40"), CommissionMoney.commission(new BigDecimal("90"), new BigDecimal("6")));
        assertEquals("15,00 AZN", CommissionMoney.azn(new BigDecimal("15")));
        assertEquals("6%", CommissionMoney.percent(new BigDecimal("6.00")));
    }

    @Test
    void panelAmountWinsAndEmptyPanelDoesNotFallThrough() {
        Visit panel = Visit.builder().bookingId(1L).finalCostAmount(new BigDecimal("90")).build();
        Visit hyper = Visit.builder().hyperRecordId(8L).finalCostAmount(new BigDecimal("250")).build();
        CommissionAmountResult ready = CommissionAmounts.resolve(List.of(panel), List.of(hyper), true);
        assertTrue(ready.ready());
        assertEquals(new BigDecimal("90.00"), ready.serviceAmount());

        Visit blank = Visit.builder().bookingId(1L).build();
        CommissionAmountResult pending = CommissionAmounts.resolve(List.of(blank), List.of(hyper), false);
        assertFalse(pending.ready());
        assertNull(pending.serviceAmount());
    }

    @Test
    void hyperSumWaitsWhenTheDayIsShared() {
        Visit first = Visit.builder().hyperRecordId(1L).finalCostAmount(new BigDecimal("100")).build();
        Visit second = Visit.builder().hyperRecordId(2L).costAmount(new BigDecimal("20")).build();
        CommissionAmountResult summed = CommissionAmounts.resolve(List.of(), List.of(first, second), false);
        assertEquals(new BigDecimal("120.00"), summed.serviceAmount());

        CommissionAmountResult shared = CommissionAmounts.resolve(List.of(), List.of(first), true);
        assertFalse(shared.ready());

        Visit zero = Visit.builder().hyperRecordId(3L).finalCostAmount(BigDecimal.ZERO).build();
        assertTrue(CommissionAmounts.resolve(List.of(), List.of(zero), false).ready());
    }

    @Test
    void rateInForceOnTheCompletionDay() {
        CommissionRate early = CommissionRate.builder().id(1L).percent(new BigDecimal("6")).effectiveFrom(LocalDate.of(2026, 1, 1)).build();
        CommissionRate next = CommissionRate.builder().id(2L).percent(new BigDecimal("8")).effectiveFrom(LocalDate.of(2026, 11, 1)).build();
        CommissionRate sameDayLaterId = CommissionRate.builder().id(4L).percent(new BigDecimal("7")).effectiveFrom(LocalDate.of(2026, 1, 1)).build();
        assertEquals(new BigDecimal("6"), CommissionRates.pick(List.of(early, next), LocalDate.of(2026, 10, 31), CommissionStatus.NEW_PERCENT));
        assertEquals(new BigDecimal("8"), CommissionRates.pick(List.of(early, next), LocalDate.of(2026, 11, 1), CommissionStatus.NEW_PERCENT));
        assertEquals(new BigDecimal("7"), CommissionRates.pick(List.of(early, sameDayLaterId), LocalDate.of(2026, 6, 1), CommissionStatus.NEW_PERCENT));
        assertEquals(CommissionStatus.OLD_PERCENT, CommissionRates.pick(List.of(), DAY, CommissionStatus.OLD_PERCENT));
    }

    @Test
    void periodDefaultsToTheWholeMonthAndKeepsBothEnds() {
        LocalDate today = LocalDate.of(2026, 1, 4);
        CommissionPeriod month = CommissionPeriod.resolve(null, null, null, today);
        assertEquals(LocalDate.of(2026, 1, 1), month.from());
        assertEquals(LocalDate.of(2026, 1, 31), month.to());
        assertEquals("month", month.preset());

        CommissionPeriod previous = CommissionPeriod.resolve("prev", null, null, today);
        assertEquals(LocalDate.of(2025, 12, 1), previous.from());
        assertEquals(LocalDate.of(2025, 12, 31), previous.to());

        CommissionPeriod week = CommissionPeriod.resolve("d7", null, null, today);
        assertEquals(LocalDate.of(2025, 12, 29), week.from());
        assertEquals(today, week.to());

        CommissionPeriod swapped = CommissionPeriod.resolve("month", LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), today);
        assertEquals(LocalDate.of(2026, 10, 1), swapped.from());
        assertEquals(LocalDate.of(2026, 10, 31), swapped.to());
        assertEquals("custom", swapped.preset());
    }
}
