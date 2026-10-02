package com.carland.carland_service.service;

import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.repository.IndividualServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * tr: Mock fərdi xidmət listesini kataloga ekler. Var olan kodu yeniden yazmaz.
 * en: Inserts the mock individual-service list into the catalog. An existing code is left as edited.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class IndividualServiceSeeder implements ApplicationRunner {

    private final IndividualServiceRepository individualServiceRepository;
    private final ServiceCategoryJson json;

    @Override
    public void run(ApplicationArguments args) {
        int added = 0;
        for (Seed seed : SEEDS) {
            if (individualServiceRepository.findByCode(seed.code()).isPresent()) {
                continue;
            }
            individualServiceRepository.save(IndividualService.builder()
                    .code(seed.code())
                    .titleJson(json.write(seed.az(), seed.en(), seed.ru()))
                    .sortOrder(seed.sort())
                    .active(true)
                    .build());
            added++;
        }
        if (added > 0) {
            log.info("INDIVIDUAL_SERVICE_SEED added {}", added);
        }
    }

    private record Seed(String code, int sort, String az, String en, String ru) {}

    private static final Seed[] SEEDS = {
            new Seed("MYF", 1, "Mühərrik yağı və filtri dəyişimi", "Engine oil and filter change", "Замена моторного масла и фильтра"),
            new Seed("HVF", 2, "Hava filtri dəyişimi", "Air filter change", "Замена воздушного фильтра"),
            new Seed("SLF", 3, "Salon filtri dəyişimi", "Cabin filter change", "Замена салонного фильтра"),
            new Seed("TBY", 4, "Təkərlərin balansı və yerdəyişməsi", "Wheel balance and rotation", "Балансировка и перестановка шин"),
            new Seed("RZV", 5, "Razval", "Wheel alignment", "Развал-схождение"),
            new Seed("EBD", 6, "Əyləc bəndləri dəyişimi", "Brake pad change", "Замена тормозных колодок"),
            new Seed("EMD", 7, "Əyləc mayesinin dəyişilməsi", "Brake fluid change", "Замена тормозной жидкости"),
            new Seed("YNF", 8, "Yanacaq filtri dəyişimi", "Fuel filter change", "Замена топливного фильтра"),
            new Seed("ASD", 9, "Alışdırma şamları dəyişimi", "Spark plug change", "Замена свечей зажигания"),
            new Seed("ANT", 10, "Soyutma mayesi (antifriz) dəyişimi", "Coolant (antifreeze) change", "Замена охлаждающей жидкости (антифриз)"),
            new Seed("SGM", 11, "Sükan gücləndirici mayesi dəyişimi", "Power steering fluid change", "Замена жидкости гидроусилителя руля"),
            new Seed("TRY", 12, "Transmissiya yağı dəyişimi", "Transmission oil change", "Замена трансмиссионного масла"),
            new Seed("KMR", 13, "Kəmər dəyişimi", "Belt change", "Замена ремня"),
            new Seed("AKM", 14, "Akkumulyator dəyişimi", "Battery change", "Замена аккумулятора"),
            new Seed("TKD", 15, "Təkərlərin dəyişimi", "Tire change", "Замена шин"),
    };
}
