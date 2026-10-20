package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminColorNameRow;
import com.carland.carland_service.dto.response.AdminColorSortRow;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * tr: Renk sırası. Üç dilin numarası ayrıdır. GET /get/color/list istenen dilin numarasını döner.
 *     catalog:colors:{lang} okuyan RedisCacheService.getOrLoadColors.
 *     Yazan: rows (boş numara), move, saveNames ve delete → evictCatalogColorsAfterCommit (catalog:colors:*).
 *     saveNames ad değiştirince, delete satırı silince o rengi kullanan sahiplerin carlist:{userId}:* anahtarını da düşer.
 *     colors.active yok: delete kaydı DB'den siler.
 * en: Color order. Each language has its own numbers. GET /get/color/list returns that language.
 *     Reader: RedisCacheService.getOrLoadColors. Writers: rows, move, saveNames, and delete evict catalog:colors:*.
 *     saveNames and delete also drop carlist:{userId}:* for owners whose cars use that color.
 *     colors has no active flag: delete removes the row from the DB.
 */
@Service
@RequiredArgsConstructor
public class ColorSortService {

    private final ColorRepository colorRepository;
    private final CarRepository carRepository;
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

    /**
     * tr: Rengi DB'den siler. colors'ta active kolonu yok. Kalan satırların üç dil sırası 1, 2, 3… olur.
     *     catalog:colors:* ve bu rengi kullanan sahiplerin carlist:{userId}:* anahtarı commit sonrası düşer.
     *     Araç hâlâ bu colorId'yi tutuyorsa FK silmeyi keser.
     * en: Deletes the color from the DB. colors has no active column. Remaining rows are renumbered 1, 2, 3… in each language.
     *     catalog:colors:* and carlist:{userId}:* for owners of this color drop after commit.
     *     A foreign key from a car that still stores this colorId blocks the delete.
     */
    @Transactional
    public void delete(Long colorId) {
        if (colorId == null) {
            throw MissingFieldException.required("colorId");
        }
        List<Color> colors = new ArrayList<>(colorRepository.findAll());
        Color target = null;
        List<Color> remaining = new ArrayList<>();
        for (Color color : colors) {
            if (colorId.equals(color.getColorId())) {
                target = color;
            } else {
                remaining.add(color);
            }
        }
        if (target == null) {
            throw new ResourceNotFoundException("color not found");
        }
        List<Long> owners = carRepository.findOwnerUserIdsByColorId(colorId);
        try {
            colorRepository.delete(target);
            colorRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Bu rəng avtomobillərdə istifadə olunur");
        }
        for (String lang : List.of("az", "en", "ru")) {
            renumber(sorted(remaining, lang), lang);
        }
        if (!remaining.isEmpty()) {
            colorRepository.saveAll(remaining);
        }
        redisCacheService.evictCatalogColorsAfterCommit();
        for (Long userId : owners) {
            if (userId != null) {
                redisCacheService.evictCarListAfterCommit(String.valueOf(userId));
            }
        }
    }

    /**
     * tr: Değişen adları ve yeni satırları yazar. Hex ve sıra ad kaydında değişmez.
     *     Yeni rengin hex'i boştur, numarası bir sonrakidir, üç dilde listenin sonuna eklenir.
     *     Ad değişen rengin sahibi carlist'i ve catalog:colors:* commit sonrası silinir.
     * en: Writes changed names and new rows. A name save does not change hex or order.
     *     A new color has an empty hex, the next id, and is appended in all three languages.
     *     A renamed color drops its owners' carlist and catalog:colors:* after commit.
     */
    @Transactional
    public void saveNames(List<AdminColorNameRow> rows) {
        List<AdminColorNameRow> requested = normalize(rows);
        if (requested.isEmpty()) {
            return;
        }
        List<Color> colors = new ArrayList<>(colorRepository.findAll());
        boolean filled = assignMissing(colors);
        Map<Long, Color> byId = new LinkedHashMap<>();
        long nextId = 0L;
        int nextAz = 0;
        int nextEn = 0;
        int nextRu = 0;
        for (Color color : colors) {
            if (color.getColorId() != null) {
                byId.put(color.getColorId(), color);
                nextId = Math.max(nextId, color.getColorId());
            }
            nextAz = Math.max(nextAz, color.getSortOrderAz() == null ? 0 : color.getSortOrderAz());
            nextEn = Math.max(nextEn, color.getSortOrderEn() == null ? 0 : color.getSortOrderEn());
            nextRu = Math.max(nextRu, color.getSortOrderRu() == null ? 0 : color.getSortOrderRu());
        }
        List<Long> renamed = new ArrayList<>();
        boolean wroteName = false;
        for (AdminColorNameRow row : requested) {
            if (row.getColorId() == null) {
                nextId++;
                nextAz++;
                nextEn++;
                nextRu++;
                colors.add(Color.builder()
                        .colorId(nextId)
                        .az(row.getAz())
                        .color(row.getEn())
                        .ru(row.getRu())
                        .hex(null)
                        .sortOrderAz(nextAz)
                        .sortOrderEn(nextEn)
                        .sortOrderRu(nextRu)
                        .build());
                wroteName = true;
                continue;
            }
            Color color = byId.get(row.getColorId());
            if (color == null) {
                throw new ResourceNotFoundException("color not found");
            }
            if (same(color.getAz(), row.getAz()) && same(color.getColor(), row.getEn()) && same(color.getRu(), row.getRu())) {
                continue;
            }
            color.setAz(row.getAz());
            color.setColor(row.getEn());
            color.setRu(row.getRu());
            renamed.add(color.getColorId());
            wroteName = true;
        }
        if (!filled && !wroteName) {
            return;
        }
        colorRepository.saveAll(colors);
        redisCacheService.evictCatalogColorsAfterCommit();
        for (Long colorId : renamed) {
            for (Long userId : carRepository.findOwnerUserIdsByColorId(colorId)) {
                if (userId != null) {
                    redisCacheService.evictCarListAfterCommit(String.valueOf(userId));
                }
            }
        }
    }

    private static List<AdminColorNameRow> normalize(List<AdminColorNameRow> rows) {
        List<AdminColorNameRow> requested = new ArrayList<>();
        if (rows == null) {
            return requested;
        }
        for (AdminColorNameRow row : rows) {
            if (row == null) {
                continue;
            }
            String az = blankToNull(row.getAz());
            String en = blankToNull(row.getEn());
            String ru = blankToNull(row.getRu());
            if (az == null && en == null && ru == null) {
                throw MissingFieldException.required("name");
            }
            requested.add(AdminColorNameRow.builder()
                    .colorId(row.getColorId())
                    .az(az)
                    .en(en)
                    .ru(ru)
                    .build());
        }
        return requested;
    }

    private static boolean same(String stored, String incoming) {
        return Objects.equals(blankToNull(stored), incoming);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
                    .nameAz(color.getAz() == null ? "" : color.getAz())
                    .nameEn(color.getColor() == null ? "" : color.getColor())
                    .nameRu(color.getRu() == null ? "" : color.getRu())
                    .hex(color.getHex() == null ? "" : color.getHex())
                    .sortOrder(stored != null ? stored : i + 1)
                    .swatch(ColorCatalogOrder.swatch(color.getHex()))
                    .build());
        }
        return rows;
    }
}
