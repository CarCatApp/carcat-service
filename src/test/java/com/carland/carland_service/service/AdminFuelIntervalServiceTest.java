package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminFuelIntervalAddRequest;
import com.carland.carland_service.dto.request.AdminFuelIntervalSaveRequest;
import com.carland.carland_service.dto.response.AdminFuelIntervalPage;
import com.carland.carland_service.dto.response.AdminFuelIntervalRow;
import com.carland.carland_service.entity.EngineType;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.IndividualServiceFilter;
import com.carland.carland_service.entity.IndividualServiceSource;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.EngineTypeRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.IndividualServiceSourceRepository;
import com.carland.carland_service.repository.MaintenanceTemplateRepository;
import com.carland.carland_service.repository.PercentagePhotoRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFuelIntervalServiceTest {

    @Mock EngineTypeRepository engineTypeRepository;
    @Mock MaintenanceTemplateRepository maintenanceTemplateRepository;
    @Mock ServiceEntityRepository serviceEntityRepository;
    @Mock IndividualServiceRepository individualServiceRepository;
    @Mock IndividualServiceSourceRepository sourceRepository;
    @Mock PercentagePhotoRepository percentagePhotoRepository;
    @Mock RedisCacheService redisCacheService;

    AdminFuelIntervalService service;
    ServiceCategoryJson json;

    @BeforeEach
    void setUp() {
        json = new ServiceCategoryJson(new ObjectMapper());
        service = new AdminFuelIntervalService(
                engineTypeRepository,
                maintenanceTemplateRepository,
                serviceEntityRepository,
                individualServiceRepository,
                sourceRepository,
                percentagePhotoRepository,
                redisCacheService,
                json);
    }

    @Test
    void openShowsTheSelectedFuelAndTheOtherFuelsThatShareTheCatalogRow() {
        EngineType diesel = engine(1L, "Diesel");
        EngineType hybrid = engine(2L, "Hybrid");
        when(engineTypeRepository.findAllByStatusOrderByEngineTypeIdAsc("ACTIVE")).thenReturn(List.of(diesel, hybrid));

        ServiceEntity spark = ServiceEntity.builder()
                .id(9L).nameAz("Buji").nameEn("Spark plugs").nameRu("Свечи")
                .intervalKm(20_000L).intervalMonth(24)
                .maintenanceTemplate(template(hybrid))
                .build();
        MaintenanceTemplate hybridTemplate = template(hybrid);
        hybridTemplate.setName("Hybrid");
        hybridTemplate.setServices(new ArrayList<>(List.of(spark)));
        when(maintenanceTemplateRepository.findByEngineType(hybrid)).thenReturn(Optional.of(hybridTemplate));

        IndividualService sparkCatalog = catalog(4L, "BUJ", "Buji", "Spark plugs", "Свечи");
        IndividualService oil = catalog(8L, "YAG", "Yağ", "Oil", "Масло");
        when(individualServiceRepository.findByActiveTrueOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(sparkCatalog, oil));
        when(sourceRepository.findAllById(any())).thenReturn(List.of(
                IndividualServiceSource.builder().serviceId(9L).individualServiceId(4L).build()));
        when(individualServiceRepository.findAllById(any())).thenReturn(List.of(sparkCatalog));
        when(sourceRepository.findByIndividualServiceIdIn(any())).thenReturn(List.of(
                IndividualServiceSource.builder().serviceId(9L).individualServiceId(4L).build(),
                IndividualServiceSource.builder().serviceId(3L).individualServiceId(4L).build()));
        ServiceEntity dieselSpark = ServiceEntity.builder().id(3L).maintenanceTemplate(template(diesel)).build();
        when(serviceEntityRepository.findWithTemplateByIdIn(any())).thenReturn(List.of(spark, dieselSpark));

        AdminFuelIntervalPage page = service.open(2L);

        assertEquals(2L, page.getSelectedEngineTypeId());
        assertEquals("Hibrid", page.getSelectedLabel());
        assertFalse(page.isTemplateMissing());
        assertEquals("Hybrid", page.getTemplateName());
        assertEquals(1, page.getRows().size());
        assertEquals("Buji", page.getRows().get(0).getTitleAz());
        assertEquals(20_000L, page.getRows().get(0).getIntervalKm());
        assertEquals("Dizel", page.getRows().get(0).getOtherFuels());
        assertEquals(1, page.getCatalogOptions().size());
        assertEquals(8L, page.getCatalogOptions().get(0).getId());
        assertEquals("Yağ", page.getCatalogOptions().get(0).getTitleAz());
    }

    @Test
    void unknownFuelFallsBackToTheFirstAndAMissingTemplateStaysEmpty() {
        EngineType diesel = engine(1L, "Diesel");
        when(engineTypeRepository.findAllByStatusOrderByEngineTypeIdAsc("ACTIVE")).thenReturn(List.of(diesel));
        when(maintenanceTemplateRepository.findByEngineType(diesel)).thenReturn(Optional.empty());

        AdminFuelIntervalPage page = service.open(99L);

        assertEquals(1L, page.getSelectedEngineTypeId());
        assertEquals("Dizel", page.getSelectedLabel());
        assertTrue(page.isTemplateMissing());
        assertTrue(page.getRows().isEmpty());
        verify(individualServiceRepository, never()).findByActiveTrueOrderBySortOrderAscIdAsc();
    }

    @Test
    void updateWritesKmAndMonthAndLeavesTheName() {
        ServiceEntity spark = ServiceEntity.builder()
                .id(9L).nameAz("Buji").intervalKm(10_000L).intervalMonth(12).build();
        when(serviceEntityRepository.findById(9L)).thenReturn(Optional.of(spark));
        when(serviceEntityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminFuelIntervalRow row = service.updateInterval(AdminFuelIntervalSaveRequest.builder()
                .serviceId(9L).intervalKm(15_000L).build());

        ArgumentCaptor<ServiceEntity> saved = ArgumentCaptor.forClass(ServiceEntity.class);
        verify(serviceEntityRepository).save(saved.capture());
        assertEquals("Buji", saved.getValue().getNameAz());
        assertEquals(15_000L, saved.getValue().getIntervalKm());
        assertNull(saved.getValue().getIntervalMonth());
        assertEquals("Buji", row.getTitleAz());
        assertEquals(15_000L, row.getIntervalKm());
        verify(percentagePhotoRepository, never()).deleteByServiceId(any());
    }

    @Test
    void updateRejectsAnEmptyInterval() {
        assertThrows(MissingFieldException.class, () -> service.updateInterval(
                AdminFuelIntervalSaveRequest.builder().serviceId(9L).build()));
        verify(serviceEntityRepository, never()).findById(any());
    }

    @Test
    void addCopiesTheSiblingKeyAndWritesThisFuelInterval() {
        EngineType hybrid = engine(2L, "Hybrid");
        EngineType diesel = engine(1L, "Diesel");
        MaintenanceTemplate hybridTemplate = template(hybrid);
        when(engineTypeRepository.findByEngineTypeId(2L)).thenReturn(hybrid);
        when(maintenanceTemplateRepository.findByEngineType(hybrid)).thenReturn(Optional.of(hybridTemplate));
        IndividualService spark = catalog(4L, "BUJ", "Buji", "Spark plugs", "Свечи");
        when(individualServiceRepository.findById(4L)).thenReturn(Optional.of(spark));
        when(sourceRepository.findByIndividualServiceIdIn(any())).thenReturn(List.of(
                IndividualServiceSource.builder().serviceId(3L).individualServiceId(4L).build()));
        when(serviceEntityRepository.findWithTemplateByIdIn(any())).thenReturn(List.of(
                ServiceEntity.builder()
                        .id(3L).serviceName("plug-key").actionType("Ignition").important(true)
                        .maintenanceTemplate(template(diesel))
                        .build()));
        when(serviceEntityRepository.save(any())).thenAnswer(invocation -> {
            ServiceEntity created = invocation.getArgument(0);
            created.setId(15L);
            return created;
        });

        service.add(AdminFuelIntervalAddRequest.builder()
                .engineTypeId(2L).individualServiceId(4L).intervalKm(20_000L).intervalMonth(24).build());

        ArgumentCaptor<ServiceEntity> saved = ArgumentCaptor.forClass(ServiceEntity.class);
        verify(serviceEntityRepository).save(saved.capture());
        assertEquals("plug-key", saved.getValue().getServiceName());
        assertEquals("Ignition", saved.getValue().getActionType());
        assertEquals("Buji", saved.getValue().getNameAz());
        assertEquals(20_000L, saved.getValue().getIntervalKm());
        assertEquals(24, saved.getValue().getIntervalMonth());
        assertTrue(saved.getValue().isImportant());
        assertEquals(hybridTemplate, saved.getValue().getMaintenanceTemplate());
        ArgumentCaptor<IndividualServiceSource> link = ArgumentCaptor.forClass(IndividualServiceSource.class);
        verify(sourceRepository).save(link.capture());
        assertEquals(15L, link.getValue().getServiceId());
        assertEquals(4L, link.getValue().getIndividualServiceId());
    }

    @Test
    void addUsesTheCatalogNameWhenTheRowIsOnNoOtherFuel() {
        EngineType petrol = engine(5L, "Petrol (Gasoline)");
        MaintenanceTemplate template = template(petrol);
        when(engineTypeRepository.findByEngineTypeId(5L)).thenReturn(petrol);
        when(maintenanceTemplateRepository.findByEngineType(petrol)).thenReturn(Optional.of(template));
        IndividualService oil = catalog(8L, "YAG", "Yağ", "Oil", "Масло");
        oil.setFilter(IndividualServiceFilter.builder().nameEn("Fluids").build());
        when(individualServiceRepository.findById(8L)).thenReturn(Optional.of(oil));
        when(sourceRepository.findByIndividualServiceIdIn(any())).thenReturn(List.of());
        when(serviceEntityRepository.save(any())).thenAnswer(invocation -> {
            ServiceEntity created = invocation.getArgument(0);
            created.setId(16L);
            return created;
        });

        service.add(AdminFuelIntervalAddRequest.builder()
                .engineTypeId(5L).individualServiceId(8L).intervalMonth(12).build());

        ArgumentCaptor<ServiceEntity> saved = ArgumentCaptor.forClass(ServiceEntity.class);
        verify(serviceEntityRepository).save(saved.capture());
        assertEquals("Oil", saved.getValue().getServiceName());
        assertEquals("Fluids", saved.getValue().getActionType());
        assertNull(saved.getValue().getIntervalKm());
        assertEquals(12, saved.getValue().getIntervalMonth());
        assertFalse(saved.getValue().isImportant());
        verify(serviceEntityRepository, never()).findWithTemplateByIdIn(any());
    }

    @Test
    void addRejectsACatalogRowAlreadyOnThatFuel() {
        EngineType hybrid = engine(2L, "Hybrid");
        when(engineTypeRepository.findByEngineTypeId(2L)).thenReturn(hybrid);
        when(maintenanceTemplateRepository.findByEngineType(hybrid)).thenReturn(Optional.of(template(hybrid)));
        when(individualServiceRepository.findById(4L)).thenReturn(Optional.of(
                catalog(4L, "BUJ", "Buji", "Spark plugs", "Свечи")));
        when(sourceRepository.findByIndividualServiceIdIn(any())).thenReturn(List.of(
                IndividualServiceSource.builder().serviceId(9L).individualServiceId(4L).build()));
        when(serviceEntityRepository.findWithTemplateByIdIn(any())).thenReturn(List.of(
                ServiceEntity.builder().id(9L).maintenanceTemplate(template(hybrid)).build()));

        assertThrows(ConflictException.class, () -> service.add(AdminFuelIntervalAddRequest.builder()
                .engineTypeId(2L).individualServiceId(4L).intervalKm(10_000L).build()));
        verify(serviceEntityRepository, never()).save(any());
    }

    @Test
    void removeDeletesTheTemplateRowAndKeepsTheCallOffPercentageCopies() {
        ServiceEntity spark = ServiceEntity.builder().id(9L).build();
        when(serviceEntityRepository.findById(9L)).thenReturn(Optional.of(spark));
        when(sourceRepository.existsById(9L)).thenReturn(true);

        service.remove(9L);

        verify(sourceRepository).deleteById(9L);
        verify(percentagePhotoRepository).deleteByServiceId(9L);
        verify(serviceEntityRepository).delete(spark);
        verify(redisCacheService).evictPercentagePhotoAfterCommit(9L);
    }

    @Test
    void removeReportsARowThatIsStillReferenced() {
        ServiceEntity spark = ServiceEntity.builder().id(9L).build();
        when(serviceEntityRepository.findById(9L)).thenReturn(Optional.of(spark));
        when(sourceRepository.existsById(9L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("fk")).when(serviceEntityRepository).delete(spark);

        assertThrows(ConflictException.class, () -> service.remove(9L));
        verify(redisCacheService, never()).evictPercentagePhotoAfterCommit(any());
    }

    @Test
    void removeOfAMissingRowDoesNotDelete() {
        when(serviceEntityRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.remove(9L));
        verify(percentagePhotoRepository, never()).deleteByServiceId(any());
    }

    private EngineType engine(Long id, String name) {
        return EngineType.builder().engineTypeId(id).engineType(name).status("ACTIVE").build();
    }

    private MaintenanceTemplate template(EngineType engine) {
        return MaintenanceTemplate.builder().id(engine.getEngineTypeId()).engineType(engine).build();
    }

    private IndividualService catalog(Long id, String code, String az, String en, String ru) {
        return IndividualService.builder()
                .id(id)
                .code(code)
                .active(true)
                .titleJson(json.write(az, en, ru))
                .build();
    }
}
