package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.ServicedBranchResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.enums.PartnerId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServicedBranchListTest {

    @Test
    void keepsBadgeOrderAndCountsHyperTemporaryBranch() {
        Visit olderHyper = Visit.builder()
                .id(1L)
                .serviceCenterId(PartnerId.HYPER.getId())
                .lastServiceDate(LocalDate.of(2025, 10, 1))
                .createdAt(LocalDateTime.of(2025, 10, 1, 8, 0))
                .build();
        Visit newestHyper = Visit.builder()
                .id(2L)
                .serviceCenterId(PartnerId.HYPER.getId())
                .lastServiceDate(LocalDate.of(2025, 11, 12))
                .createdAt(LocalDateTime.of(2025, 11, 13, 13, 38))
                .build();
        Visit panel = Visit.builder()
                .id(3L)
                .branchId(42L)
                .serviceCenterId(4L)
                .lastServiceDate(LocalDate.of(2025, 10, 3))
                .createdAt(LocalDateTime.of(2025, 10, 3, 9, 15))
                .build();
        Visit noBranch = Visit.builder().id(4L).serviceCenterId(2L).build();

        List<ServicedBranchResponse> cards = ServicedBranchList.cards(
                List.of(10L, 42L, 99L),
                List.of(panel, newestHyper, olderHyper, noBranch),
                Map.of(
                        10L, branch(10L, "Hyper Nərimanov", 40.4093, 49.8671),
                        42L, branch(42L, "AutoLux 28 May", 40.3792, 49.8486)
                ),
                "az");

        assertEquals(2, cards.size());
        assertEquals(10L, cards.get(0).getBranchId());
        assertEquals("Hyper Nərimanov", cards.get(0).getBranchName());
        assertEquals(40.4093, cards.get(0).getLat());
        assertEquals(49.8671, cards.get(0).getLng());
        assertEquals(2, cards.get(0).getCompletedServiceCount());
        assertEquals("12 Noyabr 2025 13:38", cards.get(0).getLastServiceAt());
        assertEquals(42L, cards.get(1).getBranchId());
        assertEquals(1, cards.get(1).getCompletedServiceCount());
        assertEquals("3 Oktyabr 2025 09:15", cards.get(1).getLastServiceAt());
    }

    @Test
    void englishMonthAndDateWithoutClock() {
        Visit visit = Visit.builder()
                .id(1L)
                .branchId(7L)
                .lastServiceDate(LocalDate.of(2025, 11, 12))
                .build();

        List<ServicedBranchResponse> cards = ServicedBranchList.cards(
                List.of(7L),
                List.of(visit),
                Map.of(7L, branch(7L, "Branch", null, null)),
                "en");

        assertEquals("12 November 2025", cards.get(0).getLastServiceAt());
        assertNull(cards.get(0).getLat());
    }

    private static Branch branch(Long id, String name, Double lat, Double lng) {
        return Branch.builder().id(id).name(name).lat(lat).lng(lng).build();
    }
}
