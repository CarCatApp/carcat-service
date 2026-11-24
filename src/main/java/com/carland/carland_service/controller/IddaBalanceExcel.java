package com.carland.carland_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * tr: iddaBalanceInfo Excel. Expense sütunları ekrandaki tabloyla aynıdır; JSON'da fazla alan varsa sona eklenir.
 *     Tarih metin kalır, böylece Excel yerelle AM/PM basmaz.
 * en: iddaBalanceInfo workbook. Expense columns match the on-screen table; extra JSON fields are appended.
 *     Dates stay text so Excel does not print a locale AM/PM.
 */
final class IddaBalanceExcel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> EXPENSE_FIELDS = List.of(
            "Value", "Date", "Service", "Amount", "AZN", "RequestIdentifier", "IpAddress");
    private static final List<String> PAYMENT_FIELDS = List.of("Date", "Amount");

    private IddaBalanceExcel() {
    }

    static byte[] expenses(List<JsonNode> rows) throws IOException {
        return write("Expense", columns(rows, EXPENSE_FIELDS), rows, true);
    }

    static byte[] payments(List<JsonNode> rows) throws IOException {
        return write("Payment", columns(rows, PAYMENT_FIELDS), rows, false);
    }

    static byte[] balance(byte[] raw) {
        try {
            JsonNode root = MAPPER.readTree(raw == null ? new byte[0] : raw);
            String requestId = scalar(root.get("RequestIdentifier"));
            String code = null;
            String name = null;
            String message = null;
            JsonNode status = root.get("Status");
            if (status != null && status.isObject()) {
                code = scalar(status.get("Code"));
                name = scalar(status.get("Name"));
                message = scalar(status.get("Message"));
            }
            String qepik = null;
            JsonNode response = root.get("Response");
            if (response != null && response.isObject() && response.has("Balance") && !response.get("Balance").isNull()) {
                qepik = scalar(response.get("Balance"));
            }
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Balance");
                CellStyle header = header(workbook);
                CellStyle money = money(workbook);
                String[] titles = {
                        "RequestIdentifier", "Balance (qəpik)", "AZN", "Status code", "Status name", "Status message"
                };
                writeHeader(sheet, header, List.of(titles));
                Row row = sheet.createRow(1);
                text(row, 0, requestId);
                numberOrText(row, 1, qepik, null);
                numberOrText(row, 2, manat(qepik), money);
                text(row, 3, code);
                text(row, 4, name);
                text(row, 5, message);
                size(sheet, titles.length);
                return bytes(workbook);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private static byte[] write(String sheetName, List<String> fields, List<JsonNode> rows, boolean expenseAmount)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle header = header(workbook);
            CellStyle money = money(workbook);
            List<String> titles = new ArrayList<>();
            for (String field : fields) {
                titles.add(title(field, expenseAmount));
            }
            writeHeader(sheet, header, titles);
            int index = 1;
            for (JsonNode item : rows) {
                if (item == null || !item.isObject()) {
                    continue;
                }
                Row row = sheet.createRow(index++);
                for (int column = 0; column < fields.size(); column++) {
                    String field = fields.get(column);
                    if ("AZN".equals(field)) {
                        numberOrText(row, column, manat(scalar(item.get("Amount"))), money);
                    } else if ("Amount".equals(field)) {
                        numberOrText(row, column, scalar(item.get("Amount")), null);
                    } else {
                        writeValue(row, column, item.get(field));
                    }
                }
            }
            size(sheet, fields.size());
            return bytes(workbook);
        }
    }

    private static List<String> columns(List<JsonNode> rows, List<String> first) {
        TreeSet<String> extra = new TreeSet<>();
        for (JsonNode row : rows) {
            if (row == null || !row.isObject()) {
                continue;
            }
            row.fieldNames().forEachRemaining(name -> {
                if (!first.contains(name)) {
                    extra.add(name);
                }
            });
        }
        List<String> fields = new ArrayList<>(first);
        fields.addAll(extra);
        return fields;
    }

    private static String title(String field, boolean expenseAmount) {
        if ("Amount".equals(field) && expenseAmount) {
            return "Amount (qəpik)";
        }
        return field;
    }

    private static void writeHeader(Sheet sheet, CellStyle style, List<String> titles) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < titles.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(titles.get(i));
            cell.setCellStyle(style);
        }
    }

    private static void writeValue(Row row, int column, JsonNode node) {
        if (node == null || node.isNull()) {
            text(row, column, null);
            return;
        }
        if (node.isNumber()) {
            numberOrText(row, column, node.decimalValue().toPlainString(), null);
            return;
        }
        if (node.isTextual()) {
            text(row, column, node.asText());
            return;
        }
        if (node.isBoolean()) {
            text(row, column, node.asBoolean() ? "true" : "false");
            return;
        }
        try {
            text(row, column, MAPPER.writeValueAsString(node));
        } catch (IOException ex) {
            text(row, column, node.asText());
        }
    }

    private static void numberOrText(Row row, int column, String raw, CellStyle style) {
        if (raw == null || raw.isBlank()) {
            text(row, column, null);
            return;
        }
        try {
            Cell cell = row.createCell(column);
            cell.setCellValue(new BigDecimal(raw.trim()).doubleValue());
            if (style != null) {
                cell.setCellStyle(style);
            }
        } catch (NumberFormatException ex) {
            text(row, column, raw.trim());
        }
    }

    private static void text(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private static String scalar(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue().toPlainString();
        }
        return node.asText();
    }

    private static String manat(String qepik) {
        if (qepik == null || qepik.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(qepik.trim())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .toPlainString();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static CellStyle header(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle money(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        return style;
    }

    private static void size(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) > 60 * 256) {
                sheet.setColumnWidth(i, 60 * 256);
            }
        }
    }

    private static byte[] bytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
