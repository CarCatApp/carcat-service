package com.carland.carland_service.service;

import com.carland.carland_service.entity.Color;
import com.carland.carland_service.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * tr: Mevcut renkleri silmez. Eşleşenlere az/ru/hex yazar; eskide olup yenide olmayanlar kalır;
 *     yenide olup eskide olmayanlar eklenir.
 * en: Does not delete colors. Fills az/ru/hex on matches; keeps legacy-only rows; inserts new names.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class ColorCatalogSeeder implements ApplicationRunner {

    private final ColorRepository colorRepository;
    private final RedisCacheService redisCacheService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Seed> seeds = seeds();
        long nextId = colorRepository.findAll().stream()
                .map(Color::getColorId)
                .filter(id -> id != null)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        int updated = 0;
        int inserted = 0;
        for (Seed seed : seeds) {
            Color existing = findExisting(seed);
            if (existing != null) {
                existing.setAz(seed.az);
                existing.setRu(seed.ru);
                existing.setHex(seed.hex);
                colorRepository.save(existing);
                updated++;
                continue;
            }
            if (!seed.insertIfMissing) {
                continue;
            }
            nextId++;
            colorRepository.save(Color.builder()
                    .colorId(nextId)
                    .color(seed.en)
                    .az(seed.az)
                    .ru(seed.ru)
                    .hex(seed.hex)
                    .build());
            inserted++;
        }
        redisCacheService.evictCatalogColors();
        log.info("Color catalog seed done | updated={}, inserted={}", updated, inserted);
    }

    private Color findExisting(Seed seed) {
        for (String alias : seed.aliases()) {
            Color found = colorRepository.findByColorIgnoreCase(alias);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static List<Seed> seeds() {
        return List.of(
                Seed.keep("Black", "Qara", "Чёрный", "#1C1C1C"),
                Seed.insert("Wet Asphalt", "Yaş asfalt", "Мокрый асфальт", "#34495E"),
                Seed.keep("Gray / Grey", "Boz", "Серый", "#808080", "Gray", "Grey"),
                Seed.keep("Silver", "Gümüşü", "Серебристый", "#C0C0C0"),
                Seed.keep("White", "Ağ", "Белый", "#F5F5F5"),
                Seed.keep("Beige", "Bej", "Бежевый", "#D9C3A8"),
                Seed.insert("Dark Red", "Tünd qırmızı", "Тёмно-красный", "#8B0000"),
                Seed.keep("Red", "Qırmızı", "Красный", "#C41E3A"),
                Seed.insert("Pink", "Çəhrayı", "Розовый", "#E8A0BF"),
                Seed.keep("Orange", "Narıncı", "Оранжевый", "#E67E22"),
                Seed.keep("Gold", "Qızılı", "Золотой", "#C9A227"),
                Seed.keep("Yellow", "Sarı", "Жёлтый", "#F1C40F"),
                Seed.insert("Khaki", "Xaki", "Хаки", "#C3B091"),
                Seed.insert("Dark Green", "Tünd yaşıl", "Тёмно-зелёный", "#1B4D3E"),
                Seed.keep("Green", "Yaşıl", "Зелёный", "#2E7D32"),
                Seed.insert("Light Green", "Açıq yaşıl", "Светло-зелёный", "#8BC34A"),
                Seed.insert("Light Blue", "Mavi", "Голубой", "#5DADE2"),
                Seed.keep("Blue", "Göy", "Синий", "#1E3A8A"),
                Seed.keep("Purple", "Bənövşəyi", "Фиолетовый", "#6C3483"),
                Seed.keep("Brown", "Qəhvəyi", "Коричневый", "#6B3F2B"),
                Seed.keep("Maroon", "Bordo", "Тёмно-бордовый", "#800000"),
                Seed.keep("Matte Black", "Mat qara", "Матовый чёрный", "#0A0A0A"),
                Seed.keep("Metallic silver", "Metalik gümüş", "Серебристый металлик", "#A8A9AD"),
                Seed.keep("Navy blue", "Tünd mavi", "Тёмно-синий", "#001F5B"),
                Seed.keep("Pearl white", "İncə ağ", "Жемчужно-белый", "#F8F6F0"),
                Seed.keep("Other", "Digər", "Другой", "#9E9E9E")
        );
    }

    private record Seed(String en, String az, String ru, String hex, boolean insertIfMissing, String... extraAliases) {
        static Seed keep(String en, String az, String ru, String hex, String... extraAliases) {
            return new Seed(en, az, ru, hex, false, extraAliases);
        }

        static Seed insert(String en, String az, String ru, String hex) {
            return new Seed(en, az, ru, hex, true);
        }

        String[] aliases() {
            if (extraAliases == null || extraAliases.length == 0) {
                return new String[]{en};
            }
            String[] all = new String[extraAliases.length + 1];
            all[0] = en;
            System.arraycopy(extraAliases, 0, all, 1, extraAliases.length);
            return all;
        }
    }
}
