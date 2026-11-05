package com.carland.carland_service.digitalhistory;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Raporu A4 PDF olarak basar. Yazı tipi Noto Sans'tır; Azərbaycan hərfləri gömülüdür.
 * en: Renders the report as an A4 PDF. The font is Noto Sans, with Azerbaijani letters embedded.
 */
@Component
public class DigitalHistoryPdf {

    public byte[] render(DigitalHistoryReport report) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont regular = load(document, "NotoSans-Regular.ttf");
            PDFont bold = load(document, "NotoSans-Bold.ttf");
            document.getDocumentInformation().setTitle("CarCat — Rəqəmsal tarixçə hesabatı");
            document.getDocumentInformation().setAuthor("CarCat");
            Sheet sheet = new Sheet(document, regular, bold);
            sheet.open();
            draw(sheet, report);
            sheet.finish();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("digital history pdf failed", ex);
        }
    }

    private static void draw(Sheet sheet, DigitalHistoryReport report) throws IOException {
        sheet.title("Rəqəmsal tarixçə hesabatı");
        sheet.muted("Yenilənib " + report.generatedOn());
        sheet.gap(8);
        if (!report.brand().isBlank()) {
            sheet.brand(report.brand());
        }
        if (!report.model().isBlank()) {
            sheet.model(report.model());
        }
        sheet.gap(6);
        sheet.field("Cari yürüş", report.mileage());
        sheet.field("Son servis tarixi", report.lastServiceDate());
        sheet.field("Servis qeydi", Integer.toString(report.serviceCount()));
        sheet.field("Dövr", report.period());
        sheet.field("Servis mərkəzi", Integer.toString(report.centerCount()));

        sheet.section("Texniki məlumatlar");
        sheet.field("Qeydiyyat şəhadətnaməsinin nömrəsi", report.certificateNumber());
        sheet.field("Şəhadətnamənin verilmə tarixi", report.certificateDate());
        sheet.field("Qeydiyyat nişanı", report.plate());
        sheet.field("Marka və model", report.brandModel());
        sheet.field("Ban nömrəsi (VIN)", report.vin());
        sheet.field("Buraxılış ili", report.year());
        sheet.field("Rəngi", report.color());
        sheet.field("Mühərrik həcmi", report.engineVolume());
        sheet.field("Mühərrik nömrəsi", report.engineNumber());
        sheet.field("Növü", report.bodyType());

        sheet.section("Yürüş");
        if (report.visits().isEmpty()) {
            sheet.body("Qeyd yoxdur");
        }
        for (DigitalHistoryReport.VisitBlock visit : report.visits()) {
            String when = join(visit.mileage(), visit.date());
            String label = visit.label() == null ? "" : visit.label();
            sheet.body(join(when, label));
        }

        sheet.section("Servis tarixçəsi");
        if (report.visits().isEmpty()) {
            sheet.body("Qeyd yoxdur");
        }
        for (DigitalHistoryReport.VisitBlock visit : report.visits()) {
            sheet.gap(8);
            String head = join(visit.mileage(), visit.date());
            sheet.visitHead(head.isBlank() ? "Ziyarət" : head);
            if (visit.label() != null && !visit.label().isBlank()) {
                sheet.muted(visit.label());
            }
            String place = join(visit.centerName(), visit.address());
            if (!place.isBlank()) {
                sheet.body(place);
            }
            if ((visit.packageName() != null && !visit.packageName().isBlank()) || !visit.packageLines().isEmpty()) {
                String packageHead = visit.packageName() == null || visit.packageName().isBlank()
                        ? "Paket"
                        : "Paket · " + visit.packageName();
                sheet.sub(packageHead);
                for (String line : visit.packageLines()) {
                    sheet.bullet(line);
                }
            }
            if (!visit.individualLines().isEmpty()) {
                sheet.sub("Fərdi xidmətlər");
                for (String line : visit.individualLines()) {
                    sheet.bullet(line);
                }
            }
            if ((visit.inspectionNote() != null && !visit.inspectionNote().isBlank())
                    || !visit.inspectionLines().isEmpty()) {
                sheet.sub("Təmir və diaqnostika");
                if (visit.inspectionNote() != null && !visit.inspectionNote().isBlank()) {
                    sheet.bullet(visit.inspectionNote());
                }
                for (String line : visit.inspectionLines()) {
                    sheet.bullet(line);
                }
            }
            for (String line : visit.plainLines()) {
                sheet.bullet(line);
            }
            sheet.field("Ümumi məbləğ", visit.total());
        }

        sheet.section("Ümumi servis xərci");
        sheet.body(report.grandTotal());

        sheet.section("Xidmət göstərən servis mərkəzləri");
        if (report.centers().isEmpty()) {
            sheet.body("Qeyd yoxdur");
        }
        for (DigitalHistoryReport.CenterBlock center : report.centers()) {
            sheet.gap(6);
            sheet.sub(center.name().isBlank() ? "Servis mərkəzi" : center.name());
            sheet.field("Ünvan", center.address());
            sheet.field("Telefon", center.phone());
            sheet.field("E-poçt", center.email());
            sheet.field("Ziyarət", Integer.toString(center.visits()));
            sheet.field("Ümumi xərc", center.total());
        }

        sheet.gap(12);
        sheet.body("Bu hesabat yalnız CarCat platformasına ötürülmüş məlumatlar əsasında tərtib olunub. "
                + "Bu avtomobillə bağlı bəzi hadisələr CarCat-a bildirilməmiş ola bilər.");
    }

    private static String join(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return a + " · " + b;
    }

    private static PDFont load(PDDocument document, String file) throws IOException {
        try (InputStream in = DigitalHistoryPdf.class.getResourceAsStream("/fonts/" + file)) {
            if (in == null) {
                throw new IllegalStateException("font missing: " + file);
            }
            return PDType0Font.load(document, in, true);
        }
    }

    private static final class Sheet {
        private static final float MARGIN = 48f;
        private static final float BOTTOM = 52f;
        private static final float PAGE_W = PDRectangle.A4.getWidth();

        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private PDPageContentStream stream;
        private float y;
        private int pageNo;

        private Sheet(PDDocument document, PDFont regular, PDFont bold) {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
        }

        private void open() throws IOException {
            nextPage();
        }

        private void finish() throws IOException {
            if (stream != null) {
                footer();
                stream.close();
                stream = null;
            }
        }

        private void title(String text) throws IOException {
            line(text, bold, 18f, 0f, 0.08f, 0.12f, 0.20f);
        }

        private void brand(String text) throws IOException {
            line(text, bold, 16f, 0f, 0f, 0f, 0f);
        }

        private void model(String text) throws IOException {
            line(text, regular, 13f, 0f, 0.15f, 0.15f, 0.15f);
        }

        private void section(String text) throws IOException {
            gap(12);
            line(text, bold, 13f, 0f, 0.08f, 0.16f, 0.28f);
            gap(2);
        }

        private void visitHead(String text) throws IOException {
            line(text, bold, 12f, 0f, 0f, 0f, 0f);
        }

        private void sub(String text) throws IOException {
            gap(3);
            line(text, bold, 10.5f, 0f, 0.12f, 0.12f, 0.12f);
        }

        private void body(String text) throws IOException {
            line(text == null || text.isBlank() ? " " : text, regular, 10f, 0f, 0f, 0f, 0f);
        }

        private void muted(String text) throws IOException {
            line(text == null ? "" : text, regular, 9f, 0f, 0.35f, 0.35f, 0.35f);
        }

        private void bullet(String text) throws IOException {
            line("• " + text, regular, 10f, 8f, 0f, 0f, 0f);
        }

        private void gap(float points) throws IOException {
            ensure(points);
            y -= points;
        }

        private void field(String label, String value) throws IOException {
            line(label, regular, 9f, 0f, 0.35f, 0.35f, 0.35f);
            String shown = value == null || value.isBlank() ? " " : value;
            line(shown, regular, 10.5f, 0f, 0f, 0f, 0f);
        }

        private void line(String text, PDFont font, float size, float indent, float r, float g, float b)
                throws IOException {
            float width = PAGE_W - (MARGIN * 2f) - indent;
            List<String> lines = wrap(text, font, size, width);
            float rowH = size + 4f;
            for (String row : lines) {
                ensure(rowH);
                draw(row, font, size, MARGIN + indent, y, r, g, b);
                y -= rowH;
            }
        }

        private void ensure(float height) throws IOException {
            if (y - height < BOTTOM) {
                nextPage();
            }
        }

        private void nextPage() throws IOException {
            if (stream != null) {
                footer();
                stream.close();
            }
            pageNo++;
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        private void footer() throws IOException {
            draw("CarCat · Rəqəmsal tarixçə hesabatı", regular, 8f, MARGIN, 28f, 0.4f, 0.4f, 0.4f);
            String right = "Səhifə " + pageNo;
            float width = regular.getStringWidth(right) / 1000f * 8f;
            draw(right, regular, 8f, PAGE_W - MARGIN - width, 28f, 0.4f, 0.4f, 0.4f);
        }

        private void draw(String text, PDFont font, float size, float x, float baseline, float r, float g, float b)
                throws IOException {
            if (text == null || text.isEmpty()) {
                return;
            }
            stream.setNonStrokingColor(r, g, b);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, baseline);
            stream.showText(text);
            stream.endText();
        }

        private List<String> wrap(String text, PDFont font, float size, float width) throws IOException {
            String clean = (text == null ? "" : text).replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
            if (clean.isEmpty()) {
                return List.of("");
            }
            List<String> rows = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : clean.split(" ")) {
                if (word.isEmpty()) {
                    continue;
                }
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (widthOf(candidate, font, size) <= width) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                if (!current.isEmpty()) {
                    rows.add(current.toString());
                    current.setLength(0);
                }
                if (widthOf(word, font, size) <= width) {
                    current.append(word);
                    continue;
                }
                String rest = word;
                while (!rest.isEmpty()) {
                    int cut = fit(rest, font, size, width);
                    if (cut >= rest.length()) {
                        current.append(rest);
                        break;
                    }
                    rows.add(rest.substring(0, cut));
                    rest = rest.substring(cut);
                }
            }
            if (!current.isEmpty()) {
                rows.add(current.toString());
            }
            return rows.isEmpty() ? List.of("") : rows;
        }

        private int fit(String word, PDFont font, float size, float width) throws IOException {
            int low = 1;
            int high = word.length();
            if (widthOf(word.substring(0, 1), font, size) > width) {
                return 1;
            }
            while (low < high) {
                int mid = (low + high + 1) / 2;
                if (widthOf(word.substring(0, mid), font, size) <= width) {
                    low = mid;
                } else {
                    high = mid - 1;
                }
            }
            return low;
        }

        private float widthOf(String text, PDFont font, float size) throws IOException {
            return font.getStringWidth(text) / 1000f * size;
        }
    }
}
