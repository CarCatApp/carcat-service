package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.AdminColorSortRow;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: Renk sırası. Üç dilin numarası ayrıdır. GET /get/color/list istenen dilin numarasını döner.
 *     catalog:colors:{lang} okuyan RedisCacheService.getOrLoadColors.
 *     Yazan: rows (boş numara) ve move → evictCatalogColorsAfterCommit (catalog:colors:*).
 * en: Color order. Each language has its own numbers. GET /get/color/list returns that language.
 *     Reader: RedisCacheService.getOrLoadColors. Writers: rows and move evict catalog:colors:* after commit.
 */
@Service
@RequiredArgsConstructor
public class ColorSortService {

    private final ColorRepository colorRepository;
    private final RedisCacheService redisCacheService;

    @Transactional(readOnly = true)
    public List<Color> listForApi(String acceptLanguage) {
        List<Color> colors = colorRepository.findAll();
        if (colors.isEmpty()) {
            throw new ResourceNotFoundException(
                    MessagesLangValues.COLOR_NOT_FOUND.getMessageByLang(acceptLanguage)
            );
        }
        String lang = ColorCatalogOrder.langKey(acceptLanguage);
        List<Color> sorted = sorted(colors, lang);
        List<Color> response = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Color src = sorted.get(i);
            Integer stored = ColorCatalogOrder.read(src, lang);
            response.add(Color.builder()
                    .colorId(src.getColorId())
                    .color(src.nameForLang(acceptLanguage))
                    .hex(src.getHex())
                    .sortOrder(stored != null ? stored : i + 1)
                    .build());
        }
        return response;
    }

    /**
     * tr: Admin listesi. Numarası boş olan dillere bugünkü sabit sırayı 1, 2, 3… yazar.
     * en: Admin list. Languages with a missing number get the current fixed order as 1, 2, 3…
     */
    @Transactional
    public List<AdminColorSortRow> rows(String lang) {
        String key = ColorCatalogOrder.langKey(lang);
        List<Color> colors = new ArrayList<>(colorRepository.findAll());
        if (assignMissing(colors)) {
            colorRepository.saveAll(colors);
            redisCacheService.evictCatalogColorsAfterCommit();
        }
        return toRows(sorted(colors, key), key);
    }

    /**
     * tr: Rengi o dilde verilen sıraya taşır. Hedefteki ve aradaki numaralar bir kayar.
     *     Diğer dillerin numarası değişmez.
     * en: Moves the color to the given position in that language. Numbers in between shift by one.
     *     Other languages stay as they are.
     */
    @Transactional
    public List<AdminColorSortRow> move(String lang, Long colorId, Integer position) {
        if (colorId == null) {
            throw MissingFieldException.required("colorId");
        }
        if (position == null) {
            throw MissingFieldException.required("position");
        }
        String key = ColorCatalogOrder.langKey(lang);
        List<Color> colors = new ArrayList<>(colorRepository.findAll());
        boolean filled = assignMissing(colors);
        List<Color> ordered = sorted(colors, key);
        int from = indexOf(ordered, colorId);
        if (from < 0) {
            throw new ResourceNotFoundException("color not found");
        }
        int to = Math.max(0, Math.min(position - 1, ordered.size() - 1));
        if (from != to) {
            Color moved = ordered.remove(from);
            ordered.add(to, moved);
            renumber(ordered, key);
            colorRepository.saveAll(ordered);
            redisCacheService.evictCatalogColorsAfterCommit();
        } else if (filled) {
            colorRepository.saveAll(colors);
            redisCacheService.evictCatalogColorsAfterCommit();
        }
        return toRows(ordered, key);
    }

    private boolean assignMissing(List<Color> colors) {
        boolean changed = false;
        for (String lang : List.of("az", "en", "ru")) {
            boolean missing = colors.stream().anyMatch(color -> ColorCatalogOrder.read(color, lang) == null);
            if (!missing) {
                continue;
            }
            List<Color> ordered = sorted(colors, lang);
            renumber(ordered, lang);
            changed = true;
        }
        return changed;
    }

    private static void renumber(List<Color> ordered, String lang) {
        for (int i = 0; i < ordered.size(); i++) {
            ColorCatalogOrder.write(ordered.get(i), lang, i + 1);
        }
    }

    private static List<Color> sorted(List<Color> colors, String lang) {
        List<Color> copy = new ArrayList<>(colors);
        copy.sort(ColorCatalogOrder.comparator(lang));
        return copy;
    }

    private static int indexOf(List<Color> ordered, Long colorId) {
        for (int i = 0; i < ordered.size(); i++) {
            if (colorId.equals(ordered.get(i).getColorId())) {
                return i;
            }
        }
        return -1;
    }

    private static List<AdminColorSortRow> toRows(List<Color> ordered, String lang) {
        List<AdminColorSortRow> rows = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Color color = ordered.get(i);
            Integer stored = ColorCatalogOrder.read(color, lang);
            String name = color.nameForLang(lang);
            rows.add(AdminColorSortRow.builder()
                    .colorId(color.getColorId())
                    .name(name == null ? "" : name)
                    .hex(color.getHex() == null ? "" : color.getHex())
                    .sortOrder(stored != null ? stored : i + 1)
                    .swatch(ColorCatalogOrder.swatch(color.getHex()))
                    .build());
        }
        return rows;
    }
}
