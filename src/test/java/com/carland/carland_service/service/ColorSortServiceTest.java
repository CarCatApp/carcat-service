package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminColorNameRow;
import com.carland.carland_service.dto.response.AdminColorSortRow;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColorSortServiceTest {

    @Mock ColorRepository colorRepository;
    @Mock CarRepository carRepository;
    @Mock RedisCacheService redisCacheService;

    ColorSortService service;

    @BeforeEach
    void setUp() {
        service = new ColorSortService(colorRepository, carRepository, redisCacheService);
    }

    @Test
    void moveUpShiftsTheRangeAndLeavesOtherLanguages() {
        Color black = color(1L, "Black", "Qara", "Чёрный", 1, 1, 1, "#111111");
        Color white = color(2L, "White", "Ağ", "Белый", 2, 2, 2, "#ffffff");
        Color green = color(3L, "Green", "Yaşıl", "Зелёный", 3, 5, 4, "#00aa00");
        when(colorRepository.findAll()).thenReturn(new ArrayList<>(List.of(black, white, green)));
        when(colorRepository.saveAll(any())).thenAnswer(call -> call.getArgument(0));

        List<AdminColorSortRow> rows = service.move("az", 3L, 2);

        assertEquals(2, green.getSortOrderAz());
        assertEquals(1, black.getSortOrderAz());
        assertEquals(3, white.getSortOrderAz());
        assertEquals(5, green.getSortOrderEn());
        assertEquals(4, green.getSortOrderRu());
        assertEquals(List.of("Qara", "Yaşıl", "Ağ"), rows.stream().map(AdminColorSortRow::getName).toList());
        assertEquals(List.of(1, 2, 3), rows.stream().map(AdminColorSortRow::getSortOrder).toList());
        verify(redisCacheService).evictCatalogColorsAfterCommit();
    }

    @Test
    void moveDownPullsTheRangeUp() {
        Color black = color(1L, "Black", "Qara", "Чёрный", 1, 1, 1, "#111");
        Color white = color(2L, "White", "Ağ", "Белый", 2, 2, 2, "#fff");
        Color green = color(3L, "Green", "Yaşıl", "Зелёный", 3, 3, 3, "#0a0");
        when(colorRepository.findAll()).thenReturn(new ArrayList<>(List.of(black, white, green)));
        when(colorRepository.saveAll(any())).thenAnswer(call -> call.getArgument(0));

        service.move("en", 1L, 3);

        assertEquals(3, black.getSortOrderEn());
        assertEquals(1, white.getSortOrderEn());
        assertEquals(2, green.getSortOrderEn());
        assertEquals(1, black.getSortOrderAz());
        assertEquals(1, black.getSortOrderRu());
    }

    @Test
    void apiReturnsTheRequestedLanguageOrder() {
        Color black = color(1L, "Black", "Qara", "Чёрный", 1, 3, 2, "#111111");
        Color green = color(3L, "Green", "Yaşıl", "Зелёный", 2, 1, 3, "00aa00");
        Color white = color(2L, "White", "Ağ", "Белый", 3, 2, 1, "#fff");
        when(colorRepository.findAll()).thenReturn(List.of(black, green, white));

        List<Color> en = service.listForApi("en-US");
        assertEquals(List.of("Green", "White", "Black"), en.stream().map(Color::getColor).toList());
        assertEquals(List.of(1, 2, 3), en.stream().map(Color::getSortOrder).toList());
        assertEquals("#00aa00", ColorCatalogOrder.swatch(green.getHex()));

        List<Color> ru = service.listForApi("ru");
        assertEquals(List.of("Белый", "Чёрный", "Зелёный"), ru.stream().map(Color::getColor).toList());
        assertEquals(List.of(1, 2, 3), ru.stream().map(Color::getSortOrder).toList());

        verify(colorRepository, never()).saveAll(any());
        verify(redisCacheService, never()).evictCatalogColorsAfterCommit();
    }

    @Test
    void missingNumbersFollowTheOldFixedListUntilAssigned() {
        Color other = color(9L, "Other", "Digər", "Другой", null, null, null, null);
        Color black = color(1L, "Black", "Qara", "Чёрный", null, null, null, null);
        Color green = color(3L, "Green", "Yaşıl", "Зелёный", null, null, null, null);
        when(colorRepository.findAll()).thenReturn(List.of(other, green, black));

        List<Color> az = service.listForApi("az");
        assertEquals(List.of("Qara", "Yaşıl", "Digər"), az.stream().map(Color::getColor).toList());
        assertEquals(List.of(1, 2, 3), az.stream().map(Color::getSortOrder).toList());

        List<Color> en = service.listForApi("en");
        assertEquals(List.of("Black", "Green", "Other"), en.stream().map(Color::getColor).toList());
    }

    @Test
    void openingAdminAssignsAllThreeLanguagesFromTheFixedList() {
        Color other = color(9L, "Other", "Digər", "Другой", null, null, null, "#010101");
        Color black = color(1L, "Black", "Qara", "Чёрный", null, null, null, "#111111");
        Color green = color(3L, "Green", "Yaşıl", "Зелёный", null, null, null, "#00aa00");
        when(colorRepository.findAll()).thenReturn(new ArrayList<>(List.of(other, green, black)));
        when(colorRepository.saveAll(any())).thenAnswer(call -> call.getArgument(0));

        List<AdminColorSortRow> az = service.rows("az");

        assertEquals(List.of(1, 2, 3), List.of(black.getSortOrderAz(), green.getSortOrderAz(), other.getSortOrderAz()));
        assertEquals(List.of(1, 2, 3), List.of(black.getSortOrderEn(), green.getSortOrderEn(), other.getSortOrderEn()));
        assertEquals(List.of(1, 2, 3), List.of(black.getSortOrderRu(), green.getSortOrderRu(), other.getSortOrderRu()));
        assertEquals("Qara", az.get(0).getName());
        assertEquals("#00aa00", az.get(1).getSwatch());
        verify(redisCacheService).evictCatalogColorsAfterCommit();
    }

    @Test
    void samePositionDoesNotWrite() {
        Color black = color(1L, "Black", "Qara", "Чёрный", 1, 1, 1, "#111");
        when(colorRepository.findAll()).thenReturn(new ArrayList<>(List.of(black)));

        service.move("ru", 1L, 1);

        verify(colorRepository, never()).saveAll(any());
        verify(redisCacheService, never()).evictCatalogColorsAfterCommit();
    }

    @Test
    void renameWritesNamesOnlyAndDropsOwnerLists() {
        Color black = color(1L, "Black", "Qara", "Чёрный", 1, 2, 3, "#111111");
        when(colorRepository.findAll()).thenReturn(new ArrayList<>(List.of(black)));
        when(colorRepository.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        when(carRepository.findOwnerUserIdsByColorId(1L)).thenReturn(List.of(9L, 10L));

        service.saveNames(List.of(AdminColorNameRow.builder()
                .colorId(1L)
                .az(" Qara 2 ")
                .en("Black")
                .ru("Чёрный")
                .build()));

        assertEquals("Qara 2", black.getAz());
        assertEquals("Black", black.getColor());
        assertEquals("Чёрный", black.getRu());
        assertEquals("#111111", black.getHex());
        assertEquals(1, black.getSortOrderAz());
        assertEquals(2, black.getSortOrderEn());
        assertEquals(3, black.getSortOrderRu());
        verify(redisCacheService).evictCatalogColorsAfterCommit();
        verify(redisCacheService).evictCarListAfterCommit("9");
        verify(redisCacheService).evictCarListAfterCommit("10");
    }

    @Test
    void sameNamesDoNotWrite() {
        Color black = color(1L, "Black", "Qara", "Чёрный", 1, 1, 1, "#111");
        when(colorRepository.findAll()).thenReturn(new ArrayList<>(List.of(black)));

        service.saveNames(List.of(AdminColorNameRow.builder()
                .colorId(1L)
                .az(" Qara ")
                .en("Black")
                .ru("Чёрный")
                .build()));

        verify(colorRepository, never()).saveAll(any());
        verify(redisCacheService, never()).evictCatalogColorsAfterCommit();
        verify(redisCacheService, never()).evictCarListAfterCommit(any());
    }

    @Test
    void createAppendsNameOnlyAtTheEndOfEachLanguage() {
        Color black = color(4L, "Black", "Qara", "Чёрный", 1, 3, 2, "#111");
        when(colorRepository.findAll()).thenReturn(new ArrayList<>(List.of(black)));
        when(colorRepository.saveAll(any())).thenAnswer(call -> {
            List<Color> saved = call.getArgument(0);
            Color created = saved.stream().filter(c -> Long.valueOf(5L).equals(c.getColorId())).findFirst().orElseThrow();
            assertNull(created.getHex());
            assertEquals("Yeni", created.getAz());
            assertEquals("New", created.getColor());
            assertEquals("Новый", created.getRu());
            assertEquals(2, created.getSortOrderAz());
            assertEquals(4, created.getSortOrderEn());
            assertEquals(3, created.getSortOrderRu());
            Color second = saved.stream().filter(c -> Long.valueOf(6L).equals(c.getColorId())).findFirst().orElseThrow();
            assertNull(second.getHex());
            assertEquals("İkinci", second.getAz());
            assertNull(second.getColor());
            assertEquals(3, second.getSortOrderAz());
            assertEquals(5, second.getSortOrderEn());
            assertEquals(4, second.getSortOrderRu());
            return saved;
        });

        service.saveNames(List.of(
                AdminColorNameRow.builder().az("Yeni").en("New").ru("Новый").build(),
                AdminColorNameRow.builder().az("İkinci").build()
        ));

        verify(redisCacheService).evictCatalogColorsAfterCommit();
        verify(redisCacheService, never()).evictCarListAfterCommit(any());
        verify(carRepository, never()).findOwnerUserIdsByColorId(any());
    }

    @Test
    void blankNameAndUnknownColorAreRejected() {
        assertThrows(MissingFieldException.class, () -> service.saveNames(List.of(
                AdminColorNameRow.builder().az("  ").en("").build()
        )));
        verify(colorRepository, never()).findAll();

        when(colorRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                color(1L, "Black", "Qara", "Чёрный", 1, 1, 1, "#111")
        )));
        assertThrows(ResourceNotFoundException.class, () -> service.saveNames(List.of(
                AdminColorNameRow.builder().colorId(99L).az("A").build()
        )));
        verify(colorRepository, never()).saveAll(any());
    }

    @Test
    void moveRequiresColorAndPosition() {
        assertThrows(MissingFieldException.class, () -> service.move("az", null, 1));
        assertThrows(MissingFieldException.class, () -> service.move("az", 1L, null));
        when(colorRepository.findAll()).thenReturn(new ArrayList<>());
        assertThrows(ResourceNotFoundException.class, () -> service.move("az", 4L, 1));
    }

    private static Color color(Long id, String en, String az, String ru,
                               Integer sortAz, Integer sortEn, Integer sortRu, String hex) {
        return Color.builder()
                .colorId(id)
                .color(en)
                .az(az)
                .ru(ru)
                .hex(hex)
                .sortOrderAz(sortAz)
                .sortOrderEn(sortEn)
                .sortOrderRu(sortRu)
                .build();
    }
}
