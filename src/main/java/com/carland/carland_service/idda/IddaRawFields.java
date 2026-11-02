package com.carland.carland_service.idda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * tr: idda_plate_checks.raw_json yaprakları. Anahtar yolu JSON'daki adlarla kalır
 *     (BodyNumber, ManufactoringYear). Değer, JSON metnindeki skalerdir.
 *     Excel hücresi 32767 karakterde kesilir.
 * en: Leaves of idda_plate_checks.raw_json. The key path keeps the JSON names
 *     (BodyNumber, ManufactoringYear). The value is the JSON scalar.
 *     An Excel cell is cut at 32767 characters.
 */
public final class IddaRawFields {

    public static final int EXCEL_CELL_MAX = 32767;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter EXCEL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String CUT_MARK = "\n…";

    private IddaRawFields() {
    }

    public record Leaf(String name, String value) {
    }

    public record Snapshot(Long id, LocalDateTime queriedAt, List<Leaf> leaves, String unparsed) {
    }

    public static List<Snapshot> snapshots(List<IddaPlateCheck> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Snapshot> out = new ArrayList<>(rows.size());
        for (IddaPlateCheck row : rows) {
            out.add(snapshot(row));
        }
        return List.copyOf(out);
    }

    public static Snapshot snapshot(IddaPlateCheck row) {
        Long id = row == null ? null : row.getId();
        LocalDateTime queriedAt = row == null ? null : row.getQueriedAt();
        String raw = row == null ? null : row.getRawJson();
        if (raw == null || raw.isBlank()) {
            return new Snapshot(id, queriedAt, List.of(), null);
        }
        try {
            JsonNode root = MAPPER.readTree(raw);
            List<Leaf> leaves = new ArrayList<>();
            if (root != null && !root.isMissingNode()) {
                walk("", root, leaves);
            }
            return new Snapshot(id, queriedAt, List.copyOf(leaves), null);
        } catch (Exception ex) {
            return new Snapshot(id, queriedAt, List.of(), raw);
        }
    }

    /**
     * tr: Sorgular verilen sırada. Saat Excel'deki gibi UTC+4, yyyy-MM-dd HH:mm.
     * en: Checks stay in the given order. Time matches Excel: UTC+4, yyyy-MM-dd HH:mm.
     */
    public static String excelText(List<IddaPlateCheck> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (IddaPlateCheck row : rows) {
            if (text.length() > 0) {
                text.append("\n\n");
            }
            appendSnapshot(text, snapshot(row));
        }
        return text.toString();
    }

    public static String fitExcel(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= EXCEL_CELL_MAX) {
            return text;
        }
        return text.substring(0, EXCEL_CELL_MAX - CUT_MARK.length()) + CUT_MARK;
    }

    private static void appendSnapshot(StringBuilder text, Snapshot snapshot) {
        text.append('#').append(snapshot.id() == null ? "" : snapshot.id());
        if (snapshot.queriedAt() != null) {
            text.append(' ').append(EXCEL_TIME.format(snapshot.queriedAt().plusHours(4)));
        }
        if (snapshot.unparsed() != null) {
            text.append('\n').append(snapshot.unparsed());
            return;
        }
        for (Leaf leaf : snapshot.leaves()) {
            text.append('\n').append(leaf.name()).append(": ").append(leaf.value());
        }
    }

    private static void walk(String prefix, JsonNode node, List<Leaf> leaves) {
        if (node == null || node.isNull()) {
            if (!prefix.isEmpty()) {
                leaves.add(new Leaf(prefix, "null"));
            }
            return;
        }
        if (node.isObject()) {
            if (node.isEmpty()) {
                if (!prefix.isEmpty()) {
                    leaves.add(new Leaf(prefix, "{}"));
                }
                return;
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String path = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();
                walk(path, field.getValue(), leaves);
            }
            return;
        }
        if (node.isArray()) {
            if (node.isEmpty()) {
                leaves.add(new Leaf(prefix, "[]"));
                return;
            }
            for (int i = 0; i < node.size(); i++) {
                walk(prefix + "[" + i + "]", node.get(i), leaves);
            }
            return;
        }
        leaves.add(new Leaf(prefix, scalar(node)));
    }

    private static String scalar(JsonNode node) {
        if (node.isTextual()) {
            return node.textValue();
        }
        return node.asText();
    }
}
