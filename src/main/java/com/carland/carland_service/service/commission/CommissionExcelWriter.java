package com.carland.carland_service.service.commission;

import com.carland.carland_service.dto.commission.CommissionExport;
import com.carland.carland_service.dto.commission.CommissionScreen;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * tr: İki vərəq: Tranzaksiyalar və Xülasə. Məbləğ rəqəm, faiz faiz formatı.
 * en: Two sheets: Transactions and Summary. Amounts are numbers, percents are a percent format.
 */
@Component
public class CommissionExcelWriter {

    public void write(CommissionExport export, HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle header = header(workbook);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            CellStyle percent = workbook.createCellStyle();
            percent.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

            Sheet transactions = workbook.createSheet("Tranzaksiyalar");
            String[] columns = {
                    "Tamamlanma tarixi", "Servis partnyoru", "Filial", "BAN", "Qeydiyyat nişanı",
                    "Müştəri statusu", "Xidmət dəyəri (AZN)", "Komissiya faizi", "Komissiya məbləği (AZN)",
                    "Qeyd", "Randevu"
            };
            writeHeader(transactions, header, columns);
            int rowIndex = 1;
            for (CommissionExport.Line line : export.getLines()) {
                Row row = transactions.createRow(rowIndex++);
                text(row, 0, line.getCompletedAt());
                text(row, 1, line.getPartnerName());
                text(row, 2, line.getBranchName());
                text(row, 3, line.getVin());
                text(row, 4, line.getPlate());
                text(row, 5, line.getStatusLabel());
                amount(row, 6, line.getServiceAmount(), money);
                rate(row, 7, line.getRatePercent(), percent);
                amount(row, 8, line.getCommissionAmount(), money);
                text(row, 9, line.isPending() ? "Məbləğ gözlənilir" : "");
                text(row, 10, line.getRef());
            }
            size(transactions, columns.length);

            Sheet summary = workbook.createSheet("Xülasə");
            text(summary.createRow(0), 0, "Dövr");
            text(summary.getRow(0), 1, export.getFrom() + " — " + export.getTo());
            text(summary.createRow(1), 0, "Filtrlər");
            text(summary.getRow(1), 1, export.getFilters());
            Row head = summary.createRow(3);
            String[] sumHead = {"Status", "Sayı", "Xidmət dəyəri (AZN)", "Komissiya (AZN)"};
            for (int i = 0; i < sumHead.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(sumHead[i]);
                cell.setCellStyle(header);
            }
            bucket(summary.createRow(4), "Yeni", export.getFresh(), money);
            bucket(summary.createRow(5), "Köhnə", export.getAgain(), money);
            bucket(summary.createRow(6), "Ümumi", export.getAll(), money);
            text(summary.createRow(7), 0, "Məbləğ gözlənilir");
            summary.getRow(7).createCell(1).setCellValue(export.getPendingCount());

            if (!export.getGroups().isEmpty()) {
                Row groupHead = summary.createRow(9);
                String[] groupColumns = {"Servis", "Sayı", "Xidmət dəyəri (AZN)", "Yeni komissiya", "Köhnə komissiya", "Cəmi komissiya"};
                for (int i = 0; i < groupColumns.length; i++) {
                    Cell cell = groupHead.createCell(i);
                    cell.setCellValue(groupColumns[i]);
                    cell.setCellStyle(header);
                }
                int groupRow = 10;
                for (CommissionScreen.PartnerGroup group : export.getGroups()) {
                    Row row = summary.createRow(groupRow++);
                    text(row, 0, group.getName());
                    row.createCell(1).setCellValue(group.getCount());
                    amount(row, 2, group.getService(), money);
                    amount(row, 3, group.getNewCommission(), money);
                    amount(row, 4, group.getOldCommission(), money);
                    amount(row, 5, group.getCommission(), money);
                }
            }
            size(summary, 6);

            String fileName = "CarCat_Komissiya_" + export.getFrom() + "_" + export.getTo() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            workbook.write(response.getOutputStream());
        }
    }

    private static void bucket(Row row, String label, CommissionScreen.Bucket bucket, CellStyle money) {
        text(row, 0, label);
        row.createCell(1).setCellValue(bucket.getCount());
        amount(row, 2, bucket.getService(), money);
        amount(row, 3, bucket.getCommission(), money);
    }

    private static CellStyle header(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static void writeHeader(Sheet sheet, CellStyle style, String[] columns) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(style);
        }
    }

    private static void text(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null || "—".equals(value) ? "" : value);
    }

    private static void amount(Row row, int column, BigDecimal value, CellStyle style) {
        if (value == null) {
            row.createCell(column).setCellValue("");
            return;
        }
        Cell cell = row.createCell(column);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private static void rate(Row row, int column, BigDecimal percent, CellStyle style) {
        Cell cell = row.createCell(column);
        if (percent == null) {
            cell.setCellValue("");
            return;
        }
        cell.setCellValue(percent.doubleValue() / 100d);
        cell.setCellStyle(style);
    }

    private static void size(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
