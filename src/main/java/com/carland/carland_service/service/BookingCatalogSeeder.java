package com.carland.carland_service.service;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchPackage;
import com.carland.carland_service.entity.BranchService;
import com.carland.carland_service.entity.BranchServiceBrand;
import com.carland.carland_service.entity.BookingCancelReason;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.repository.BranchPackageRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServiceBrandRepository;
import com.carland.carland_service.repository.BranchServiceRepository;
import com.carland.carland_service.repository.BookingCancelReasonRepository;
import com.carland.carland_service.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * tr: Mevcut Hyper partner şubelerine katalog + iptal sebepleri. Yeni partner satırı yok.
 * en: Catalog + cancel reasons on existing Hyper partner branches. No new partner row.
 */
@Slf4j
@Component
@Order(21)
@RequiredArgsConstructor
public class BookingCatalogSeeder implements ApplicationRunner {

    static final String PKG_HYPER_EXTRA = "pkg:hyper-extra";
    static final String SVC_OIL = "svc:oil-change";

    private final PartnerRepository partnerRepository;
    private final BranchRepository branchRepository;
    private final BranchPackageRepository packageRepository;
    private final BranchServiceRepository serviceRepository;
    private final BranchServiceBrandRepository brandRepository;
    private final BookingCancelReasonRepository cancelReasonRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedCancelReasons();
        Optional<Partner> hyper = partnerRepository.findBySourceIgnoreCaseAndActiveTrue("hyper");
        if (hyper.isEmpty()) {
            log.info("BOOKING_CATALOG_SEED_SKIP no active hyper partner");
            return;
        }
        List<Branch> branches = branchRepository.findByPartnerOrderByIdAsc(hyper.get());
        if (branches.isEmpty()) {
            log.info("BOOKING_CATALOG_SEED_SKIP partnerId={} no branches", hyper.get().getId());
            return;
        }
        for (Branch branch : branches) {
            seedBranch(branch);
        }
        log.info("BOOKING_CATALOG_SEEDED partnerId={} branches={}", hyper.get().getId(), branches.size());
    }

    private void seedBranch(Branch branch) {
        Long branchId = branch.getId();
        if (!serviceRepository.existsByBranchIdAndServiceKey(branchId, SVC_OIL)) {
            BranchService oil = serviceRepository.save(BranchService.builder()
                    .branch(branch)
                    .serviceKey(SVC_OIL)
                    .kind("SERVICE")
                    .titleJson(titles("Yağ dəyişimi", "Oil change", "Замена масла"))
                    .priceMin(3500)
                    .priceMax(7500)
                    .currency("AZN")
                    .durationMin(30)
                    .active(true)
                    .build());
            brandRepository.save(BranchServiceBrand.builder().branchService(oil).brand("Castrol").build());
            brandRepository.save(BranchServiceBrand.builder().branchService(oil).brand("Mobil").build());
        }
        seedDirection(branch, "dir:repair", "Təmir", "Repair", "Ремонт");
        seedDirection(branch, "dir:inspection", "Diaqnostika", "Inspection", "Диагностика");
        seedDirection(branch, "dir:trade", "Alqı-satqı", "Trade", "Трейд-ин");
        if (!packageRepository.existsByBranchIdAndServiceKey(branchId, PKG_HYPER_EXTRA)) {
            packageRepository.save(BranchPackage.builder()
                    .branch(branch)
                    .serviceKey(PKG_HYPER_EXTRA)
                    .titleJson(titles("Hyper Extra", "Hyper Extra", "Hyper Extra"))
                    .priceMin(12900)
                    .priceMax(12900)
                    .currency("AZN")
                    .durationMin(120)
                    .includedServiceKeys("[\"svc:oil-change\",\"svc:air-filter\",\"svc:cabin-filter\"]")
                    .active(true)
                    .build());
        }
    }

    private void seedDirection(Branch branch, String key, String az, String en, String ru) {
        if (serviceRepository.existsByBranchIdAndServiceKey(branch.getId(), key)) {
            return;
        }
        serviceRepository.save(BranchService.builder()
                .branch(branch)
                .serviceKey(key)
                .kind("DIRECTION")
                .titleJson(titles(az, en, ru))
                .currency("AZN")
                .active(true)
                .build());
    }

    private void seedCancelReasons() {
        insertReason("change_of_plans", 1, titles("Plan dəyişdi", "Change of plans", "Планы изменились"));
        insertReason("found_another_branch", 2, titles("Başqa şöbə", "Found another branch", "Другой филиал"));
        insertReason("price", 3, titles("Qiymət", "Price", "Цена"));
        insertReason("other", 4, titles("Digər", "Other", "Другое"));
    }

    private void insertReason(String code, int sort, String titleJson) {
        if (cancelReasonRepository.existsByCode(code)) {
            return;
        }
        cancelReasonRepository.save(BookingCancelReason.builder()
                .code(code)
                .titleJson(titleJson)
                .sortOrder(sort)
                .active(true)
                .build());
    }

    private static String titles(String az, String en, String ru) {
        return "{\"az\":\"" + az + "\",\"en\":\"" + en + "\",\"ru\":\"" + ru + "\"}";
    }
}
