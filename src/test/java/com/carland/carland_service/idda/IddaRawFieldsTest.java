package com.carland.carland_service.idda;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IddaRawFieldsTest {

    @Test
    void leavesKeepIddaNamesAndScalarValues() {
        IddaRawFields.Snapshot snapshot = IddaRawFields.snapshot(check(7L, at(), successJson()));

        assertEquals(List.of(
                "RequestIdentifier",
                "Status.Name",
                "Status.Code",
                "Status.Message",
                "Response.Certificate.Number",
                "Response.Certificate.GivenDate",
                "Response.Car.NumberPlate",
                "Response.Car.Mark",
                "Response.Car.BodyNumber",
                "Response.Car.ManufactoringYear",
                "Response.Car.Color",
                "Response.Car.EngineCapacity",
                "Response.Car.EngineNumber",
                "Response.Car.Type.Id",
                "Response.Car.Type.Description"
        ), snapshot.leaves().stream().map(IddaRawFields.Leaf::name).toList());
        assertEquals("19XZE4F54NE012640", value(snapshot, "Response.Car.BodyNumber"));
        assertEquals("2022", value(snapshot, "Response.Car.ManufactoringYear"));
        assertEquals("Gümüşü metallik", value(snapshot, "Response.Car.Color"));
        assertEquals("0", value(snapshot, "Status.Code"));
        assertEquals("", value(snapshot, "Status.Message"));
        assertEquals("3", value(snapshot, "Response.Car.Type.Id"));
        assertNull(snapshot.unparsed());
    }

    @Test
    void nullOwnerArrayAndBrokenJsonStayLiteral() {
        IddaRawFields.Snapshot blocked = IddaRawFields.snapshot(check(1L, at(),
                "{\"Status\":{\"Name\":\"ServiceRestrictedByPerson\",\"Code\":4},\"Response\":null}"));
        assertEquals("null", value(blocked, "Response"));
        assertEquals("4", value(blocked, "Status.Code"));

        IddaRawFields.Snapshot owner = IddaRawFields.snapshot(check(2L, at(),
                "{\"Response\":{\"Owner\":{\"Name\":\"Ali\",\"Surname\":\"Veli\",\"Patronymic\":\"Oglu\"},\"Car\":{\"Type\":{}}}}"));
        assertEquals("Ali", value(owner, "Response.Owner.Name"));
        assertEquals("Veli", value(owner, "Response.Owner.Surname"));
        assertEquals("Oglu", value(owner, "Response.Owner.Patronymic"));
        assertEquals("{}", value(owner, "Response.Car.Type"));

        IddaRawFields.Snapshot items = IddaRawFields.snapshot(check(3L, at(),
                "{\"Items\":[\"a\",{\"K\":1}]}"));
        assertEquals("a", value(items, "Items[0]"));
        assertEquals("1", value(items, "Items[1].K"));

        IddaRawFields.Snapshot broken = IddaRawFields.snapshot(check(4L, at(), "{"));
        assertEquals("{", broken.unparsed());
        assertTrue(broken.leaves().isEmpty());

        IddaRawFields.Snapshot missing = IddaRawFields.snapshot(check(5L, at(), "  "));
        assertTrue(missing.leaves().isEmpty());
        assertNull(missing.unparsed());
    }

    @Test
    void excelTextListsEveryCheckNewestAsGiven() {
        String text = IddaRawFields.excelText(List.of(
                check(9L, LocalDateTime.of(2026, 11, 2, 6, 0),
                        "{\"Response\":{\"Car\":{\"BodyNumber\":\"AAA\"}}}"),
                check(8L, LocalDateTime.of(2026, 10, 1, 5, 30),
                        "{\"Status\":{\"Code\":4},\"Response\":null}")
        ));

        assertEquals("""
                #9 2026-11-02 10:00
                Response.Car.BodyNumber: AAA

                #8 2026-10-01 09:30
                Status.Code: 4
                Response: null""", text);
    }

    @Test
    void excelCellStopsAtTheSpreadsheetLimit() {
        String fitted = IddaRawFields.fitExcel("a".repeat(IddaRawFields.EXCEL_CELL_MAX + 20));

        assertEquals(IddaRawFields.EXCEL_CELL_MAX, fitted.length());
        assertTrue(fitted.endsWith("\n…"));
    }

    private static String value(IddaRawFields.Snapshot snapshot, String name) {
        return snapshot.leaves().stream()
                .filter(leaf -> leaf.name().equals(name))
                .map(IddaRawFields.Leaf::value)
                .findFirst()
                .orElseThrow();
    }

    private static LocalDateTime at() {
        return LocalDateTime.of(2026, 11, 2, 6, 0);
    }

    private static IddaPlateCheck check(Long id, LocalDateTime queriedAt, String rawJson) {
        return IddaPlateCheck.builder()
                .id(id)
                .queriedAt(queriedAt)
                .rawJson(rawJson)
                .build();
    }

    private static String successJson() {
        return "{\"RequestIdentifier\":\"8ec23951-d0ae-4d7d-a6c1-0b9eb1bbff0f\","
                + "\"Status\":{\"Name\":\"Successful\",\"Code\":0,\"Message\":\"\"},"
                + "\"Response\":{\"Certificate\":{\"Number\":\"BA535286\",\"GivenDate\":\"25.11.2025\"},"
                + "\"Car\":{\"NumberPlate\":\"77BU669\",\"Mark\":\"HONDA INSIGHT MINIK\","
                + "\"BodyNumber\":\"19XZE4F54NE012640\",\"ManufactoringYear\":\"2022\","
                + "\"Color\":\"Gümüşü metallik\",\"EngineCapacity\":\"1496\",\"EngineNumber\":\"3513075\","
                + "\"Type\":{\"Id\":3,\"Description\":\"MINIK\"}}}}";
    }
}
