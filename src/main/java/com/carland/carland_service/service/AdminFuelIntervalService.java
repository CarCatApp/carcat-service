package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminFuelIntervalAddRequest;
import com.carland.carland_service.dto.request.AdminFuelIntervalSaveRequest;
import com.carland.carland_service.dto.response.AdminFuelCatalogOption;
import com.carland.carland_service.dto.response.AdminFuelIntervalPage;
import com.carland.carland_service.dto.response.AdminFuelIntervalRow;
import com.carland.carland_service.dto.response.AdminFuelTypeOption;
import com.carland.carland_service.entity.EngineType;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.IndividualServiceSource;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.enums.EngineTypeTranslation;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.EngineTypeRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.IndividualServiceSourceRepository;
import com.carland.carland_service.repository.MaintenanceTemplateRepository;
import com.carland.carland_service.repository.PercentagePhotoRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * tr: Yakıt şablonunun km və ay aralığı. {@code services} satırına yazır.
 *     Redis: {@code percentages} və {@code carlist} dəyişmir. Çıxarılan kalemin
 *     {@code photo:percentage:{serviceId}} açarı commit-dən sonra silinir.
 *     Köhnə faiz sətirləri olduğu kimi qalır. Faiz ekranı şablonu canlı oxuyur.
 * en: Km and month interval of a fuel template. Writes the {@code services} row.
 *     Redis: {@code percentages} and {@code carlist} stay. Removing a row drops
 *     {@code photo:percentage:{serviceId}} after commit.
 *     Existing percentage rows are left as they are. The percentage screen reads the template live.
 */
@Service
@RequiredArgsConstructor
public class AdminFuelIntervalService {

    private static final long KM_MAX = 2_000_000L;
    private static final int MONTH_MAX = 240;
    private static final int NAME_MAX = 240;

    private final EngineTypeRepository engineTypeRepository;
    private final MaintenanceTemplateRepository maintenanceTemplateRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final IndividualServiceRepository individualServiceRepository;
    private final IndividualServiceSourceRepository sourceRepository;
    private final PercentagePhotoRepository percentagePhotoRepository;
    private final RedisCacheService redisCacheService;
    private final ServiceCategoryJson json;

    /**
     * tr: Aktiv yakıtlar və seçilmiş şablonun kalemləri. Bilinməyən id birinci yakıta düşür.
     * en: Active fuels and the selected template's rows. An unknown id falls back to the first fuel.
     */
    @Transactional(readOnly = true)
    public AdminFuelIntervalPage open(Long engineTypeId) {
        List<EngineType> engines = engineTypeRepository.findAllByStatusOrderByEngineTypeIdAsc("ACTIVE");
        if (engines == null) {
            engines = List.of();
        }
        List<AdminFuelTypeOption> fuels = new ArrayList<>();
        EngineType selectedEngine = null;
        for (EngineType engine : engines) {
            if (engine == null || engine.getEngineTypeId() == null) {
                continue;
            }
            String label = EngineTypeTranslation.translate(engine.getEngineType(), "az");
            fuels.add(AdminFuelTypeOption.builder()
                    .engineTypeId(engine.getEngineTypeId())
                    .label(label)
                    .build());
            if (engine.getEngineTypeId().equals(engineTypeId)) {
                selectedEngine = engine;
            }
        }
        if (selectedEngine == null && !fuels.isEmpty()) {
            Long firstId = fuels.get(0).getEngineTypeId();
            for (EngineType engine : engines) {
                if (engine != null && firstId.equals(engine.getEngineTypeId())) {
                    selectedEngine = engine;
                    break;
                }
            }
        }
        AdminFuelIntervalPage page = AdminFuelIntervalPage.builder()
                .fuels(fuels)
                .templateMissing(true)
                .build();
        if (selectedEngine == null) {
            return page;
        }
        page.setSelectedEngineTypeId(selectedEngine.getEngineTypeId());
        page.setSelectedLabel(EngineTypeTranslation.translate(selectedEngine.getEngineType(), "az"));
        MaintenanceTemplate template = maintenanceTemplateRepository.findByEngineType(selectedEngine).orElse(null);
        if (template == null) {
            return page;
        }
        page.setTemplateMissing(false);
        page.setTemplateName(template.getName());
        List<ServiceEntity> services = distinctServices(template.getServices());
        services.sort(Comparator
                .comparing((ServiceEntity row) -> text(row.getNameAz()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ServiceEntity::getId));
        List<IndividualService> activeCatalog = individualServiceRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
        if (activeCatalog == null) {
            activeCatalog = List.of();
        }
        Map<Long, Long> catalogByService = catalogByService(services);
        page.setRows(rowsOf(services, catalogByService, selectedEngine.getEngineTypeId()));
        page.setCatalogOptions(optionsNotOnFuel(activeCatalog, new HashSet<>(catalogByService.values())));
        return page;
    }

    /**
     * tr: Yalnız bu sətrin km və ayını dəyişir. Ad və faiz kopyası durur.
     * en: Changes only this row's km and month. The name and the percentage copy stay.
     */
    @Transactional
    public AdminFuelIntervalRow updateInterval(AdminFuelIntervalSaveRequest body) {
        if (body == null || body.getServiceId() == null) {
            throw MissingFieldException.required("serviceId");
        }
        Intervals intervals = requireIntervals(body.getIntervalKm(), body.getIntervalMonth());
        ServiceEntity service = serviceEntityRepository.findById(body.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("xidmət tapılmadı"));
        service.setIntervalKm(intervals.km());
        service.setIntervalMonth(intervals.month());
        return toRow(serviceEntityRepository.save(service), null, "");
    }

    /**
     * tr: Aktiv kataloq sətirini bu yakıtın şablonuna bağlayır. Adı kataloqdan kopyalayır.
     *     Km və ay yalnız bu yakıta yazılır. Başqa yakıtdakı eyni sətrin serviceName və actionType kopyalanır.
     * en: Links an active catalog row onto this fuel's template. The name is copied from the catalog.
     *     Km and month are stored for this fuel only. serviceName and actionType are copied from the same row on another fuel.
     */
    @Transactional
    public AdminFuelIntervalRow add(AdminFuelIntervalAddRequest body) {
        if (body == null || body.getEngineTypeId() == null) {
            throw MissingFieldException.required("engineTypeId");
        }
        if (body.getIndividualServiceId() == null) {
            throw MissingFieldException.required("individualServiceId");
        }
        Intervals intervals = requireIntervals(body.getIntervalKm(), body.getIntervalMonth());
        EngineType engine = engineTypeRepository.findByEngineTypeId(body.getEngineTypeId());
        if (engine == null || !"ACTIVE".equals(engine.getStatus())) {
            throw new ResourceNotFoundException("yakıt növü tapılmadı");
        }
        MaintenanceTemplate template = maintenanceTemplateRepository.findByEngineType(engine)
                .orElseThrow(() -> new ResourceNotFoundException("bu yakıtın şablonu yoxdur"));
        IndividualService catalog = individualServiceRepository.findById(body.getIndividualServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("kataloq xidməti tapılmadı"));
        if (!Boolean.TRUE.equals(catalog.getActive())) {
            throw new ResourceNotFoundException("kataloq xidməti tapılmadı");
        }
        ServiceEntity sibling = otherFuelSibling(linkedServices(catalog.getId()), engine.getEngineTypeId());
        Map<String, String> title = json.read(catalog.getTitleJson());
        String nameAz = clip(title.get("az"), NAME_MAX);
        String nameEn = clip(title.get("en"), NAME_MAX);
        String nameRu = clip(title.get("ru"), NAME_MAX);
        String serviceName = firstText(sibling == null ? null : sibling.getServiceName(), nameEn, nameAz);
        if (serviceName.isBlank()) {
            throw new MissingFieldException("kataloq adı boşdur");
        }
        String actionType = firstText(
                sibling == null ? null : sibling.getActionType(),
                catalog.getFilter() == null ? null : catalog.getFilter().getNameEn(),
                "Service");
        ServiceEntity created = serviceEntityRepository.save(ServiceEntity.builder()
                .serviceName(clip(serviceName, 255))
                .actionType(clip(actionType, 64))
                .nameAz(nameAz)
                .nameEn(nameEn)
                .nameRu(nameRu)
                .intervalKm(intervals.km())
                .intervalMonth(intervals.month())
                .important(sibling != null && sibling.isImportant())
                .maintenanceTemplate(template)
                .build());
        if (created.getId() == null) {
            throw new IllegalStateException("service id was not assigned");
        }
        sourceRepository.save(IndividualServiceSource.builder()
                .serviceId(created.getId())
                .individualServiceId(catalog.getId())
                .build());
        return toRow(created, catalog, "");
    }

    /**
     * tr: Sətri yalnız bu şablondan silir. Kataloq, faiz kopyası və digər yakıtlar durur.
     * en: Deletes the row from this template only. The catalog, percentage copies and other fuels stay.
     */
    @Transactional
    public void remove(Long serviceId) {
        if (serviceId == null) {
            throw MissingFieldException.required("serviceId");
        }
        ServiceEntity service = serviceEntityRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("xidmət tapılmadı"));
        if (sourceRepository.existsById(serviceId)) {
            sourceRepository.deleteById(serviceId);
        }
        percentagePhotoRepository.deleteByServiceId(serviceId);
        sourceRepository.flush();
        try {
            serviceEntityRepository.delete(service);
            serviceEntityRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("bu sətir başqa yazılara bağlıdır, silinmədi");
        }
        redisCacheService.evictPercentagePhotoAfterCommit(serviceId);
    }

    private Map<Long, Long> catalogByService(List<ServiceEntity> services) {
        if (services.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> catalogByService = new HashMap<>();
        List<Long> serviceIds = services.stream().map(ServiceEntity::getId).toList();
        for (IndividualServiceSource link : sourceRepository.findAllById(serviceIds)) {
            if (link != null && link.getServiceId() != null && link.getIndividualServiceId() != null) {
                catalogByService.put(link.getServiceId(), link.getIndividualServiceId());
            }
        }
        return catalogByService;
    }

    private List<AdminFuelIntervalRow> rowsOf(List<ServiceEntity> services, Map<Long, Long> catalogByService,
                                              Long selectedEngineTypeId) {
        if (services.isEmpty()) {
            return List.of();
        }
        Map<Long, IndividualService> catalogs = catalogsById(catalogByService.values());
        Map<Long, String> otherFuels = otherFuelLabels(catalogByService, selectedEngineTypeId);
        List<AdminFuelIntervalRow> rows = new ArrayList<>();
        for (ServiceEntity service : services) {
            Long catalogId = catalogByService.get(service.getId());
            rows.add(toRow(service, catalogs.get(catalogId), otherFuels.getOrDefault(catalogId, "")));
        }
        return rows;
    }

    private Map<Long, String> otherFuelLabels(Map<Long, Long> catalogByService, Long selectedEngineTypeId) {
        if (catalogByService.isEmpty()) {
            return Map.of();
        }
        List<IndividualServiceSource> links = sourceRepository.findByIndividualServiceIdIn(
                new ArrayList<>(catalogByService.values()));
        if (links == null || links.isEmpty()) {
            return Map.of();
        }
        List<Long> siblingIds = new ArrayList<>();
        for (IndividualServiceSource link : links) {
            if (link != null && link.getServiceId() != null) {
                siblingIds.add(link.getServiceId());
            }
        }
        Map<Long, ServiceEntity> siblings = new HashMap<>();
        if (!siblingIds.isEmpty()) {
            List<ServiceEntity> loaded = serviceEntityRepository.findWithTemplateByIdIn(siblingIds);
            if (loaded != null) {
                for (ServiceEntity sibling : loaded) {
                    if (sibling != null && sibling.getId() != null) {
                        siblings.put(sibling.getId(), sibling);
                    }
                }
            }
        }
        Map<Long, Set<String>> labels = new HashMap<>();
        for (IndividualServiceSource link : links) {
            if (link == null || link.getIndividualServiceId() == null) {
                continue;
            }
            ServiceEntity sibling = siblings.get(link.getServiceId());
            if (sibling == null || sameEngine(sibling, selectedEngineTypeId)) {
                continue;
            }
            String label = engineLabel(sibling);
            if (label.isBlank()) {
                continue;
            }
            labels.computeIfAbsent(link.getIndividualServiceId(), ignored -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))
                    .add(label);
        }
        Map<Long, String> joined = new HashMap<>();
        for (Map.Entry<Long, Set<String>> entry : labels.entrySet()) {
            joined.put(entry.getKey(), String.join(", ", entry.getValue()));
        }
        return joined;
    }

    private List<AdminFuelCatalogOption> optionsNotOnFuel(List<IndividualService> catalogs, Set<Long> onFuel) {
        List<AdminFuelCatalogOption> options = new ArrayList<>();
        for (IndividualService catalog : catalogs) {
            if (catalog == null || catalog.getId() == null || onFuel.contains(catalog.getId())) {
                continue;
            }
            Map<String, String> title = json.read(catalog.getTitleJson());
            options.add(AdminFuelCatalogOption.builder()
                    .id(catalog.getId())
                    .code(catalog.getCode())
                    .titleAz(firstText(title.get("az"), catalog.getCode()))
                    .build());
        }
        return options;
    }

    private Map<Long, IndividualService> catalogsById(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, IndividualService> byId = new HashMap<>();
        for (IndividualService catalog : individualServiceRepository.findAllById(new ArrayList<>(ids))) {
            if (catalog != null && catalog.getId() != null) {
                byId.put(catalog.getId(), catalog);
            }
        }
        return byId;
    }

    private List<ServiceEntity> linkedServices(Long catalogId) {
        List<IndividualServiceSource> links = sourceRepository.findByIndividualServiceIdIn(List.of(catalogId));
        if (links == null || links.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (IndividualServiceSource link : links) {
            if (link != null && link.getServiceId() != null) {
                ids.add(link.getServiceId());
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        List<ServiceEntity> loaded = serviceEntityRepository.findWithTemplateByIdIn(ids);
        return loaded == null ? List.of() : loaded;
    }

    private ServiceEntity otherFuelSibling(List<ServiceEntity> siblings, Long engineTypeId) {
        ServiceEntity picked = null;
        for (ServiceEntity candidate : siblings) {
            if (candidate == null) {
                continue;
            }
            if (sameEngine(candidate, engineTypeId)) {
                throw new ConflictException("bu xidmət artıq bu yakıtdadır");
            }
            if (picked == null || idOf(candidate) < idOf(picked)) {
                picked = candidate;
            }
        }
        return picked;
    }

    private AdminFuelIntervalRow toRow(ServiceEntity service, IndividualService catalog, String otherFuels) {
        Map<String, String> title = catalog == null ? Map.of() : json.read(catalog.getTitleJson());
        return AdminFuelIntervalRow.builder()
                .serviceId(service.getId())
                .titleAz(firstText(service.getNameAz(), title.get("az")))
                .titleEn(firstText(service.getNameEn(), title.get("en")))
                .titleRu(firstText(service.getNameRu(), title.get("ru")))
                .intervalKm(service.getIntervalKm())
                .intervalMonth(service.getIntervalMonth())
                .otherFuels(otherFuels == null ? "" : otherFuels)
                .build();
    }

    private static List<ServiceEntity> distinctServices(List<ServiceEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, ServiceEntity> unique = new LinkedHashMap<>();
        for (ServiceEntity row : rows) {
            if (row != null && row.getId() != null) {
                unique.putIfAbsent(row.getId(), row);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static boolean sameEngine(ServiceEntity service, Long engineTypeId) {
        Long id = engineId(service);
        return id != null && id.equals(engineTypeId);
    }

    private static Long engineId(ServiceEntity service) {
        if (service == null || service.getMaintenanceTemplate() == null
                || service.getMaintenanceTemplate().getEngineType() == null) {
            return null;
        }
        return service.getMaintenanceTemplate().getEngineType().getEngineTypeId();
    }

    private static String engineLabel(ServiceEntity service) {
        if (service == null || service.getMaintenanceTemplate() == null
                || service.getMaintenanceTemplate().getEngineType() == null) {
            return "";
        }
        return text(EngineTypeTranslation.translate(
                service.getMaintenanceTemplate().getEngineType().getEngineType(), "az"));
    }

    private static Intervals requireIntervals(Long km, Integer month) {
        if (km == null && month == null) {
            throw new MissingFieldException("km və ya ay lazımdır");
        }
        if (km != null && (km < 1 || km > KM_MAX)) {
            throw new MissingFieldException("km 1 ilə 2000000 arasında olmalıdır");
        }
        if (month != null && (month < 1 || month > MONTH_MAX)) {
            throw new MissingFieldException("ay 1 ilə 240 arasında olmalıdır");
        }
        return new Intervals(km, month);
    }

    private static long idOf(ServiceEntity service) {
        return service.getId() == null ? Long.MAX_VALUE : service.getId();
    }

    private static String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String clip(String value, int max) {
        String trimmed = text(value);
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private record Intervals(Long km, Integer month) {}
}
