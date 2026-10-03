package com.carland.carland_service.service;

import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndividualCatalogSyncTest {

    @Mock ServiceEntityRepository serviceEntityRepository;
    @Mock IndividualServiceRepository individualServiceRepository;
    @Mock BranchIndividualServiceRepository branchServiceRepository;

    IndividualCatalogSync sync;

    @BeforeEach
    void setUp() {
        sync = new IndividualCatalogSync(
                serviceEntityRepository,
                individualServiceRepository,
                branchServiceRepository,
                new ServiceCategoryJson(new ObjectMapper()));
    }

    @Test
    void sameNamesCollapseDespiteIntervalAndCase() {
        ServiceEntity first = service(2L, "Mühərrik yağı", "Engine oil", "Масло", 10000L, 12);
        ServiceEntity second = service(9L, "mühərrik   yağı", "ENGINE OIL", "масло", 15000L, 6);
        ServiceEntity other = service(3L, "Hava filtri", "Air filter", "Фильтр", 20000L, 12);

        List<IndividualCatalogSync.Desired> rows = IndividualCatalogSync.collect(List.of(second, other, first));

        assertEquals(2, rows.size());
        assertEquals("Hava filtri", rows.get(0).az());
        assertEquals("Mühərrik yağı", rows.get(1).az());
        assertEquals(2L, rows.get(1).sourceId());
        assertTrue(rows.get(0).code().matches("[A-Z0-9]{8}"));
        assertEquals(rows.get(0).code(), IndividualCatalogSync.codeFor(rows.get(0).key(), new java.util.HashSet<>()));
    }

    @Test
    void blankNamesAreSkipped() {
        assertTrue(IndividualCatalogSync.collect(List.of(service(1L, "  ", "", null, 1L, 1))).isEmpty());
    }

    @Test
    void syncDropsMockPricesAndDeactivatesNamesMissingFromTemplates() {
        ServiceEntity oil = service(4L, "Yağ", "Oil", "Масло", 10000L, 12);
        when(serviceEntityRepository.findAll()).thenReturn(List.of(oil));
        IndividualService mock = IndividualService.builder().id(15L).code("MYF").active(true).sortOrder(1).build();
        when(individualServiceRepository.findByCode(any())).thenReturn(Optional.empty());
        when(individualServiceRepository.findByCode("MYF")).thenReturn(Optional.of(mock));
        IndividualService leftover = IndividualService.builder().id(8L).code("ABCD1234").active(true).sortOrder(2).build();
        when(individualServiceRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(leftover));

        sync.sync();

        verify(branchServiceRepository).deleteByIndividualService_Id(15L);
        verify(individualServiceRepository).delete(mock);
        ArgumentCaptor<IndividualService> saved = ArgumentCaptor.forClass(IndividualService.class);
        verify(individualServiceRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        assertTrue(saved.getAllValues().stream().anyMatch(row ->
                "Yağ".equals(titleAz(row)) && Boolean.TRUE.equals(row.getActive())));
        assertTrue(saved.getAllValues().stream().anyMatch(row ->
                "ABCD1234".equals(row.getCode()) && Boolean.FALSE.equals(row.getActive())));
        verify(branchServiceRepository, never()).deleteByIndividualService_Id(8L);
    }

    @Test
    void normalizeDropsSpaceAndCase() {
        assertEquals(IndividualCatalogSync.normalize("Engine  OIL"), IndividualCatalogSync.normalize("engine oil"));
        assertFalse(IndividualCatalogSync.normalize("Air").equals(IndividualCatalogSync.normalize("Oil")));
    }

    private static String titleAz(IndividualService row) {
        String json = row.getTitleJson();
        if (json == null || !json.contains("\"az\"")) {
            return "";
        }
        int start = json.indexOf("\"az\":\"") + 6;
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    private static ServiceEntity service(Long id, String az, String en, String ru, Long km, Integer months) {
        return ServiceEntity.builder()
                .id(id)
                .nameAz(az)
                .nameEn(en)
                .nameRu(ru)
                .intervalKm(km)
                .intervalMonth(months)
                .build();
    }
}
