package com.carland.carland_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * tr: Eski şube başlıklarını bir kez siler ve boş kataloğa 10 ortak başlık yazar.
 * en: Drops the old per-branch headings once and writes 10 shared headings when the catalog is empty.
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class BrandModelHeadingSeeder implements ApplicationRunner {

    private record Seed(String az, String en, String ru, boolean oil) {}

    private static final Seed[] SEED = {
            new Seed("Yağlar", "Oils", "Масла", true),
            new Seed("Filtrlər", "Filters", "Фильтры", false),
            new Seed("Əyləc qəlibləri", "Brake pads", "Тормозные колодки", false),
            new Seed("Əyləc mayeləri", "Brake fluids", "Тормозные жидкости", false),
            new Seed("Əyləc Diskləri", "Brake discs", "Тормозные диски", false),
            new Seed("Soyutma mayesi (antifriz)", "Coolant (antifreeze)", "Охлаждающая жидкость (антифриз)", false),
            new Seed("Təkərlər", "Tires", "Шины", false),
            new Seed("Qayışlar", "Belts", "Ремни", false),
            new Seed("Akkumulyatorlar", "Batteries", "Аккумуляторы", false),
            new Seed("Digər ehtiyat hissələri", "Other spare parts", "Прочие запчасти", false)
    };

    private final JdbcTemplate jdbc;
    private final ServiceCategoryJson json;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (columnExists("brand_model_services", "branch_id")) {
            jdbc.update("DELETE FROM brand_models");
            jdbc.update("DELETE FROM brand_model_services");
            jdbc.execute("ALTER TABLE brand_model_services DROP COLUMN IF EXISTS branch_id");
            log.info("BRAND_MODEL_HEADINGS dropped per-branch headings");
        }
        jdbc.execute("ALTER TABLE brand_model_services DROP COLUMN IF EXISTS title");
        if (!columnExists("brand_model_services", "title_json")) {
            jdbc.execute("ALTER TABLE brand_model_services ADD COLUMN title_json varchar(1024)");
        }
        jdbc.execute("DROP TABLE IF EXISTS branch_goods");
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM brand_model_services", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        int sort = 1;
        for (Seed row : SEED) {
            jdbc.update(
                    "INSERT INTO brand_model_services (title_json, oil, sort_order) VALUES (?, ?, ?)",
                    json.write(row.az(), row.en(), row.ru()),
                    row.oil(),
                    sort++);
        }
        log.info("BRAND_MODEL_HEADINGS seeded {}", SEED.length);
    }

    private boolean columnExists(String table, String column) {
        Integer found = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                table,
                column);
        return found != null && found > 0;
    }
}
