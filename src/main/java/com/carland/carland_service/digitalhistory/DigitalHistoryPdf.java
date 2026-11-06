package com.carland.carland_service.digitalhistory;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * tr: Rəqəmsal tarixçəni şablon səhifəsinə basır. Hesabat nömrəsi hələlik sabitdir.
 *     «Yenilənib» Bakı saatıdır. QR bloku şəkli ilə birlikdə sabitdir.
 * en: Prints the digital history onto the template pages. The report number is still fixed.
 *     "Yenilənib" is Baku time. The QR block, image included, is fixed.
 */
@Component
public class DigitalHistoryPdf {

    static final String REPORT_NO = "CC-2025-0001849";

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(
                new com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi());
    }

    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM.yy");

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float LEFT = 48.2f;
    private static final float CONTENT_W = 498.9f;
    private static final float RIGHT = LEFT + CONTENT_W;
    private static final float FLOW_TOP = 64f;
    private static final float FLOW_LIMIT = 788f;

    private static final float INK_R = 11f / 255f;
    private static final float INK_G = 12f / 255f;
    private static final float INK_B = 14f / 255f;
    private static final float MUTED_R = 95f / 255f;
    private static final float MUTED_G = 101f / 255f;
    private static final float MUTED_B = 112f / 255f;
    private static final float HEADER_R = 0.067f;
    private static final float HEADER_G = 0.078f;
    private static final float HEADER_B = 0.098f;
    private static final float LINE_R = 0.898f;
    private static final float LINE_G = 0.906f;
    private static final float LINE_B = 0.918f;
    private static final float STRIP_R = 0.965f;
    private static final float STRIP_G = 0.965f;
    private static final float STRIP_B = 0.961f;
    private static final float ORANGE_R = 193f / 255f;
    private static final float ORANGE_G = 65f / 255f;
    private static final float ORANGE_B = 12f / 255f;
    private static final float CHECK_R = 0.18f;
    private static final float CHECK_G = 0.45f;
    private static final float CHECK_B = 0.32f;

    private static final String VERIFY_TITLE = "Hesabatın orijinallığını yoxlayın";
    private static final String VERIFY_LINK =
            "QR kodu skan edin və ya daxil olun: carcat.az/verify/" + REPORT_NO;
    private static final String VERIFY_BODY =
            "Bu hesabat yalnız CarCat platformasına ötürülmüş məlumatlar əsasında tərtib olunub. "
                    + "Bu avtomobillə bağlı bəzi hadisələr CarCat-a bildirilməmiş ola bilər. "
                    + "Hesabatdan avtomobilin texniki baxışı ilə yanaşı, qərarvermə üçün köməkçi vasitə kimi istifadə edin. "
                    + "(c) 2026 CarCat MMC.";

    public byte[] render(DigitalHistoryReport report) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont regular = loadFont(document, "NotoSans-Regular.ttf");
            PDFont bold = loadFont(document, "NotoSans-Bold.ttf");
            document.getDocumentInformation().setTitle("CarCat — Rəqəmsal tarixçə hesabatı");
            document.getDocumentInformation().setAuthor("CarCat");
            Canvas canvas = new Canvas(document, regular, bold, report);
            canvas.draw();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("digital history pdf failed", ex);
        }
    }

    private static PDFont loadFont(PDDocument document, String file) throws IOException {
        try (InputStream in = DigitalHistoryPdf.class.getResourceAsStream("/fonts/" + file)) {
            if (in == null) {
                throw new IllegalStateException("font missing: " + file);
            }
            return PDType0Font.load(document, in, true);
        }
    }

    private static byte[] resource(String name) throws IOException {
        try (InputStream in = DigitalHistoryPdf.class.getResourceAsStream("/digitalhistory/" + name)) {
            if (in == null) {
                throw new IllegalStateException("pdf asset missing: " + name);
            }
            return in.readAllBytes();
        }
    }

    private final class Canvas {
        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private final DigitalHistoryReport report;
        private final PDImageXObject logo;
        private final PDImageXObject logoInk;
        private final PDImageXObject mascot;
        private final PDImageXObject plate;
        private final PDImageXObject qr;
        private final PDImageXObject photo;
        private final List<PDPage> pages = new ArrayList<>();
        private final String stampedAt;
        private PDPageContentStream stream;
        private float top;
        private float lastDotPdfY = -1f;

        private Canvas(PDDocument document, PDFont regular, PDFont bold, DigitalHistoryReport report)
                throws IOException {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            this.report = report;
            this.logo = image("carcat-logo.png");
            this.logoInk = image("carcat-logo-ink.png");
            this.mascot = image("carcat-mascot.png");
            this.plate = image("plate-badge.png");
            this.qr = image("verify-qr.png");
            this.photo = photo(report.photo());
            this.stampedAt = STAMP.format(ZonedDateTime.now(BAKU));
        }

        private void draw() throws IOException {
            cover();
            history();
            totalBar();
            centers();
            verify();
            closeStream();
            footers();
        }

        private void cover() throws IOException {
            open(false);
            fill(0, PAGE_H - 82.2f, PAGE_W, 82.2f, HEADER_R, HEADER_G, HEADER_B);
            fit(logo, LEFT, 28.3f, 92f, 25.5f, false);
            String title = "Rəqəmsal tarixçə hesabatı";
            text(title, bold, 9.5f, RIGHT - width(title, bold, 9.5f), 27.4f, 1f, 1f, 1f);
            String stamp = "№ " + REPORT_NO + "    Yenilənib " + stampedAt;
            float stampSize = fitSize(stamp, regular, 9f, CONTENT_W - 120f);
            text(stamp, regular, stampSize, RIGHT - width(stamp, regular, stampSize), 45.2f, 1f, 1f, 1f);

            float titleMax = photo == null ? CONTENT_W : 230f;
            String brand = blank(report.brand()) ? report.model() : report.brand();
            String model = blank(report.brand()) ? "" : report.model();
            float brandSize = fitSize(brand, bold, 30f, titleMax);
            text(brand, bold, brandSize, LEFT, 122.8f, INK_R, INK_G, INK_B);
            if (!blank(model)) {
                float modelSize = fitSize(model, bold, 30f, titleMax);
                text(model, bold, modelSize, LEFT, 154.3f, INK_R, INK_G, INK_B);
            }
            if (photo != null) {
                fit(photo, 289f, 107.7f, 258.1f, 135.6f, true);
            }
            drawPlate();

            round(LEFT, 263.2f, CONTENT_W, 77.7f, 12f, 1f, 1f, 1f, true);
            float col = CONTENT_W / 3f;
            vline(LEFT + col, 277f, 327f);
            vline(LEFT + col * 2f, 277f, 327f);
            stat(LEFT, "Cari yürüş", report.mileage(), dateLine(report.lastServiceDate()));
            stat(LEFT + col, "Servis qeydi", Integer.toString(report.serviceCount()), report.lastServiceDate());
            stat(LEFT + col * 2f, "Servis mərkəzi", Integer.toString(report.centerCount()), "");

            text("Texniki məlumatlar", bold, 11f, LEFT, 362.9f, INK_R, INK_G, INK_B);
            spec(0, "Qeydiyyat şəhadətnaməsinin nömrəsi", report.certificateNumber());
            spec(1, "Şəhadətnamənin verilmə tarixi", report.certificateDate());
            spec(2, "Qeydiyyat nişanı", report.plate());
            spec(3, "Marka və model", report.brandModel());
            spec(4, "Ban nömrəsi (VIN)", report.vin());
            spec(5, "Buraxılış ili", report.year());
            spec(6, "Rəngi", report.color());
            spec(7, "Mühərrik həcmi", report.engineVolume());
            spec(8, "Mühərrik nömrəsi", report.engineNumber());
            spec(9, "Növü", report.bodyType());
            fit(mascot, 371.5f, 398.7f, 181.4f, 221.9f, false);

            chart();
        }

        private void drawPlate() throws IOException {
            float x = LEFT;
            float y = 199.2f;
            float w = 133.2f;
            float h = 29.8f;
            stream.drawImage(plate, x, PAGE_H - y - h, w, h);
            String value = blank(report.plate()) ? "" : report.plate();
            if (value.isEmpty()) {
                return;
            }
            float area = w * 0.64f;
            float size = fitSize(value, bold, 14f, area);
            float textX = x + w * 0.30f;
            float baseline = PAGE_H - y - (h + size * 0.35f) / 2f;
            raw(value, bold, size, textX, baseline, INK_R, INK_G, INK_B);
        }

        private void stat(float x, String label, String value, String sub) throws IOException {
            text(label, regular, 8.5f, x + 14.8f, 278.2f, MUTED_R, MUTED_G, MUTED_B);
            text(blank(value) ? "—" : value, bold, 18f, x + 14.8f, 293.3f, INK_R, INK_G, INK_B);
            if (!blank(sub)) {
                text(sub, regular, 8.5f, x + 14.8f, 317.3f, MUTED_R, MUTED_G, MUTED_B);
            }
        }

        private void spec(int index, String label, String value) throws IOException {
            float y = 391.9f + index * 23.55f;
            text(label, regular, 9.5f, LEFT, y, MUTED_R, MUTED_G, MUTED_B);
            hairline(LEFT, y + 16.5f, 352f);
            if (blank(value)) {
                return;
            }
            float size = fitSize(value, regular, 9.5f, 150f);
            text(value, regular, size, 348f - width(value, regular, size), y, INK_R, INK_G, INK_B);
        }

        private void chart() throws IOException {
            float y = 640.4f;
            float h = 143.9f;
            round(LEFT, y, CONTENT_W, h, 12f, 1f, 1f, 1f, true);
            text("Yürüş dinamikası", bold, 11f, LEFT + 14.8f, y + 15.6f, INK_R, INK_G, INK_B);
            List<Spot> spots = spots();
            String average = average(spots);
            if (!average.isEmpty()) {
                float size = 8.5f;
                text(average, regular, size, RIGHT - 16f - width(average, regular, size), y + 17.6f,
                        MUTED_R, MUTED_G, MUTED_B);
            }
            if (spots.size() >= 2) {
                plot(spots, y);
            }
            legend(y + h - 20f);
        }

        private void plot(List<Spot> spots, float cardTop) throws IOException {
            int min = spots.stream().mapToInt(Spot::km).min().orElse(0);
            int max = spots.stream().mapToInt(Spot::km).max().orElse(min);
            float plotLeft = LEFT + 36f;
            float plotRight = RIGHT - 28f;
            float plotTop = cardTop + 46f;
            float plotBottom = cardTop + 100f;
            int last = spots.size() - 1;
            float[] xs = new float[spots.size()];
            float[] ys = new float[spots.size()];
            for (int i = 0; i < spots.size(); i++) {
                xs[i] = spots.size() == 1 ? (plotLeft + plotRight) / 2f
                        : plotLeft + (plotRight - plotLeft) * i / last;
                float t = max == min ? 0.5f : (spots.get(i).km() - min) / (float) (max - min);
                ys[i] = PAGE_H - (plotBottom - t * (plotBottom - plotTop));
            }
            stream.setStrokingColor(INK_R, INK_G, INK_B);
            stream.setLineWidth(1.05f);
            stream.moveTo(xs[0], ys[0]);
            for (int i = 1; i < spots.size(); i++) {
                stream.lineTo(xs[i], ys[i]);
            }
            stream.stroke();
            float slot = spots.size() < 2 ? 80f : (plotRight - plotLeft) / last;
            int step = slot >= 46f ? 1 : (int) Math.ceil(46f / slot);
            for (int i = 0; i < spots.size(); i++) {
                if (spots.get(i).repair()) {
                    disk(xs[i], ys[i], 3.4f, ORANGE_R, ORANGE_G, ORANGE_B, true);
                } else {
                    disk(xs[i], ys[i], 3.4f, INK_R, INK_G, INK_B, false);
                }
                if (i % step != 0 && i != last) {
                    continue;
                }
                String km = number(spots.get(i).km());
                String month = MONTH.format(spots.get(i).date());
                float kmW = width(km, bold, 7.5f);
                float monthW = width(month, regular, 7.5f);
                raw(km, bold, 7.5f, xs[i] - kmW / 2f, ys[i] + 7f, INK_R, INK_G, INK_B);
                text(month, regular, 7.5f, xs[i] - monthW / 2f, plotBottom + 6f, MUTED_R, MUTED_G, MUTED_B);
            }
        }

        private void legend(float y) throws IOException {
            disk(LEFT + 22f, PAGE_H - y - 4f, 3.2f, INK_R, INK_G, INK_B, false);
            text("Planlı / qulluq ziyarəti", regular, 7.5f, LEFT + 30f, y, MUTED_R, MUTED_G, MUTED_B);
            disk(LEFT + 148f, PAGE_H - y - 4f, 3.2f, ORANGE_R, ORANGE_G, ORANGE_B, true);
            text("Təmir ziyarəti", regular, 7.5f, LEFT + 156f, y, MUTED_R, MUTED_G, MUTED_B);
        }

        private void history() throws IOException {
            open(true);
            text("Servis tarixçəsi", bold, 18f, LEFT, top, INK_R, INK_G, INK_B);
            top += 28f;
            String note = "Qeydlər ən yenidən ən köhnəyə doğru sıralanıb – son ziyarət yuxarıdadır.";
            for (String row : wrap(note, regular, 9.5f, CONTENT_W)) {
                text(row, regular, 9.5f, LEFT, top, MUTED_R, MUTED_G, MUTED_B);
                top += 13f;
            }
            top += 8f;
            if (report.visits().isEmpty()) {
                text("Qeyd yoxdur", regular, 10f, LEFT, top, INK_R, INK_G, INK_B);
                top += 18f;
                return;
            }
            for (DigitalHistoryReport.VisitBlock visit : report.visits()) {
                visitCard(visit);
                top += 10f;
            }
        }

        private void visitCard(DigitalHistoryReport.VisitBlock visit) throws IOException {
            List<Row> rows = rows(visit);
            String head = head(visit);
            List<String> headLines = wrap(head, bold, 11f, 350f);
            float headerH = 14f + headLines.size() * 15f + 6f;
            int index = 0;
            boolean first = true;
            while (first || index < rows.size()) {
                float room = FLOW_LIMIT - top;
                float need = headerH + 30f + (rows.isEmpty() ? 0 : 22f);
                if (room < need && top > FLOW_TOP + 8f) {
                    open(true);
                    room = FLOW_LIMIT - top;
                }
                int take = 0;
                float used = headerH;
                while (index + take < rows.size()) {
                    float rowH = rowHeight(rows.get(index + take));
                    if (used + rowH + 30f > room && take > 0) {
                        break;
                    }
                    used += rowH;
                    take++;
                    if (used + 30f >= room) {
                        break;
                    }
                }
                boolean last = index + take >= rows.size();
                float height = headerH + 30f;
                for (int i = 0; i < take; i++) {
                    height += rowHeight(rows.get(index + i));
                }
                float cardTop = top;
                cardShell(LEFT + 104.8f, cardTop, 394f, height);
                float pdfHead = PAGE_H - cardTop - 22f;
                if (first) {
                    dot(143f, pdfHead, repair(visit));
                }
                float textX = LEFT + 104.8f + 26f;
                float lineTop = cardTop + 12f;
                pin(LEFT + 104.8f + 12f, PAGE_H - lineTop - 12f);
                for (String line : headLines) {
                    text(line, bold, 11f, textX, lineTop, INK_R, INK_G, INK_B);
                    lineTop += 15f;
                }
                lineTop = cardTop + headerH;
                for (int i = 0; i < take; i++) {
                    Row row = rows.get(index + i);
                    float rowH = rowHeight(row);
                    icon(row.wrench(), LEFT + 104.8f + 12f, PAGE_H - lineTop - 11f);
                    float cursor = lineTop;
                    for (String line : wrap(row.name(), regular, 9.5f, row.detail().isEmpty() ? 340f : 210f)) {
                        text(line, regular, 9.5f, textX, cursor, INK_R, INK_G, INK_B);
                        cursor += 12.5f;
                    }
                    if (!row.detail().isEmpty()) {
                        String detail = row.detail();
                        float detailSize = fitSize(detail, regular, 8.5f, 150f);
                        float dx = LEFT + 104.8f + 394f - 12f - width(detail, regular, detailSize);
                        text(detail, regular, detailSize, dx, lineTop, MUTED_R, MUTED_G, MUTED_B);
                    }
                    lineTop += rowH;
                }
                String amount = blank(visit.total()) ? "—" : visit.total();
                float amountSize = fitSize(amount, bold, 11f, 180f);
                float barTop = cardTop + height - 30f;
                text("Ümumi məbləğ", regular, 8.5f, LEFT + 104.8f + 14f, barTop + 9f, MUTED_R, MUTED_G, MUTED_B);
                text(amount, bold, amountSize,
                        LEFT + 104.8f + 394f - 14f - width(amount, bold, amountSize),
                        barTop + 8f, INK_R, INK_G, INK_B);
                if (first) {
                    String km = blank(visit.mileage()) ? "" : visit.mileage();
                    String date = blank(visit.date()) ? "" : visit.date();
                    if (!km.isEmpty()) {
                        text(km, bold, 11f, 132f - width(km, bold, 11f), cardTop + 8f, INK_R, INK_G, INK_B);
                    }
                    if (!date.isEmpty()) {
                        text(date, regular, 7.5f, 132f - width(date, regular, 7.5f), cardTop + 24f,
                                MUTED_R, MUTED_G, MUTED_B);
                    }
                }
                top = cardTop + height;
                index += take;
                first = false;
                if (take == 0) {
                    break;
                }
            }
        }

        private void cardShell(float x, float cardTop, float w, float h) throws IOException {
            stream.saveGraphicsState();
            traceRound(x, PAGE_H - cardTop - h, w, h, 8f);
            stream.clip();
            fill(x, PAGE_H - cardTop - h, w, h, 1f, 1f, 1f);
            fill(x, PAGE_H - cardTop - h, w, 30f, STRIP_R, STRIP_G, STRIP_B);
            stream.restoreGraphicsState();
            stream.setStrokingColor(LINE_R, LINE_G, LINE_B);
            stream.setLineWidth(0.9f);
            traceRound(x, PAGE_H - cardTop - h, w, h, 8f);
            stream.stroke();
        }

        private void totalBar() throws IOException {
            if (top + 78f > FLOW_LIMIT) {
                open(true);
            }
            top += 8f;
            float h = 61.7f;
            float y = PAGE_H - top - h;
            traceRound(LEFT, y, CONTENT_W, h, 14f);
            stream.setNonStrokingColor(INK_R, INK_G, INK_B);
            stream.fill();
            text("Ümumi servis xərci", regular, 11f, LEFT + 20f, top + 24f, 1f, 1f, 1f);
            String amount = blank(report.grandTotal()) ? "—" : report.grandTotal();
            float size = fitSize(amount, bold, 22f, 250f);
            text(amount, bold, size, RIGHT - 20f - width(amount, bold, size), top + 18f, 1f, 1f, 1f);
            top += h;
        }

        private void centers() throws IOException {
            if (top + 40f > FLOW_LIMIT) {
                open(true);
            }
            top += 18f;
            text("Xidmət göstərən servis mərkəzləri", bold, 11f, LEFT, top, INK_R, INK_G, INK_B);
            top += 16f;
            if (report.centers().isEmpty()) {
                text("Qeyd yoxdur", regular, 10f, LEFT, top, INK_R, INK_G, INK_B);
                top += 16f;
                return;
            }
            float gap = 14.1f;
            float cardW = (CONTENT_W - gap) / 2f;
            List<DigitalHistoryReport.CenterBlock> centers = report.centers();
            for (int i = 0; i < centers.size(); i += 2) {
                DigitalHistoryReport.CenterBlock left = centers.get(i);
                DigitalHistoryReport.CenterBlock right = i + 1 < centers.size() ? centers.get(i + 1) : null;
                float h = Math.max(centerHeight(left, cardW), right == null ? 0 : centerHeight(right, cardW));
                if (top + h > FLOW_LIMIT) {
                    open(true);
                }
                centerCard(left, LEFT, top, cardW, h);
                if (right != null) {
                    centerCard(right, LEFT + cardW + gap, top, cardW, h);
                }
                top += h + 12f;
            }
        }

        private void centerCard(DigitalHistoryReport.CenterBlock center, float x, float cardTop, float w, float h)
                throws IOException {
            round(x, cardTop, w, h, 12f, 1f, 1f, 1f, true);
            float y = cardTop + 14f;
            text(blank(center.name()) ? "Servis mərkəzi" : center.name(), bold, 11f, x + 14.8f, y, INK_R, INK_G, INK_B);
            y += 20f;
            for (String line : centerLines(center)) {
                for (String wrapped : wrap(line, regular, 8.5f, w - 30f)) {
                    text(wrapped, regular, 8.5f, x + 14.8f, y, MUTED_R, MUTED_G, MUTED_B);
                    y += 13.1f;
                }
            }
            float statsTop = cardTop + h - 48f;
            hairline(x + 14.8f, statsTop, x + w - 14.8f);
            String visits = Integer.toString(center.visits());
            text(visits, bold, 9.5f, x + 14.8f, statsTop + 8f, INK_R, INK_G, INK_B);
            text("ziyarət", regular, 7.5f, x + 14.8f, statsTop + 22f, MUTED_R, MUTED_G, MUTED_B);
            String money = centerMoney(center.total());
            float moneySize = fitSize(money, bold, 9.5f, w * 0.55f);
            text(money, bold, moneySize, x + w - 14.8f - width(money, bold, moneySize), statsTop + 8f,
                    INK_R, INK_G, INK_B);
            String caption = "ümumi xərc";
            text(caption, regular, 7.5f, x + w - 14.8f - width(caption, regular, 7.5f), statsTop + 22f,
                    MUTED_R, MUTED_G, MUTED_B);
        }

        private float centerHeight(DigitalHistoryReport.CenterBlock center, float w) throws IOException {
            float lines = 0;
            for (String line : centerLines(center)) {
                lines += wrap(line, regular, 8.5f, w - 30f).size() * 13.1f;
            }
            return 14f + 20f + lines + 56f;
        }

        private List<String> centerLines(DigitalHistoryReport.CenterBlock center) {
            List<String> lines = new ArrayList<>();
            if (!blank(center.address())) {
                lines.add(center.address());
            }
            if (!blank(center.phone())) {
                String phone = center.phone().trim();
                lines.add(phone.toLowerCase(Locale.ROOT).startsWith("tel") ? phone : "Tel " + phone);
            }
            if (!blank(center.email())) {
                lines.add(center.email());
            }
            return lines;
        }

        private void verify() throws IOException {
            List<String> body = wrap(VERIFY_BODY, regular, 7.5f, CONTENT_W - 86f);
            float h = Math.max(84f, 36f + body.size() * 11.2f);
            if (top + 16f + h > FLOW_LIMIT) {
                open(true);
            }
            top += 6f;
            round(LEFT, top, CONTENT_W, h, 10f, 1f, 1f, 1f, true);
            float qrSize = 56f;
            stream.drawImage(qr, LEFT + 14f, PAGE_H - top - 14f - qrSize, qrSize, qrSize);
            float textX = LEFT + 84f;
            text(VERIFY_TITLE, bold, 11f, textX, top + 12f, INK_R, INK_G, INK_B);
            text(VERIFY_LINK, regular, 8.5f, textX, top + 28f, MUTED_R, MUTED_G, MUTED_B);
            float y = top + 44f;
            for (String line : body) {
                text(line, regular, 7.5f, textX, y, MUTED_R, MUTED_G, MUTED_B);
                y += 11.2f;
            }
            top += h;
        }

        private void open(boolean continuation) throws IOException {
            closeStream();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            pages.add(page);
            stream = new PDPageContentStream(document, page);
            lastDotPdfY = -1f;
            top = continuation ? FLOW_TOP : 0f;
            if (continuation) {
                miniHeader();
            }
        }

        private void miniHeader() throws IOException {
            fit(logoInk, LEFT, 30f, 70f, 16f, false);
            String identity = identity();
            if (!identity.isEmpty()) {
                float size = fitSize(identity, regular, 7.5f, CONTENT_W - 86f);
                text(identity, regular, size, RIGHT - width(identity, regular, size), 33.4f, INK_R, INK_G, INK_B);
            }
            hairline(LEFT, 54f, RIGHT);
            top = FLOW_TOP;
        }

        private void footers() throws IOException {
            int total = pages.size();
            String left = "CarCat · Rəqəmsal tarixçə hesabatı · " + REPORT_NO;
            for (int i = 0; i < total; i++) {
                try (PDPageContentStream footer = new PDPageContentStream(
                        document, pages.get(i), AppendMode.APPEND, true, true)) {
                    footer.setNonStrokingColor(MUTED_R, MUTED_G, MUTED_B);
                    footer.beginText();
                    footer.setFont(regular, 7.5f);
                    footer.newLineAtOffset(LEFT, 28f);
                    footer.showText(left);
                    footer.endText();
                    String right = "Səhifə " + (i + 1) + " / " + total;
                    float w = width(right, regular, 7.5f);
                    footer.beginText();
                    footer.setFont(regular, 7.5f);
                    footer.newLineAtOffset(RIGHT - w, 28f);
                    footer.showText(right);
                    footer.endText();
                }
            }
        }

        private void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        private void dot(float x, float pdfY, boolean repair) throws IOException {
            if (lastDotPdfY > 0f) {
                stream.setStrokingColor(0.75f, 0.77f, 0.80f);
                stream.setLineWidth(1f);
                stream.moveTo(x, lastDotPdfY);
                stream.lineTo(x, pdfY);
                stream.stroke();
            }
            if (repair) {
                disk(x, pdfY, 4.2f, ORANGE_R, ORANGE_G, ORANGE_B, true);
            } else {
                disk(x, pdfY, 4.2f, INK_R, INK_G, INK_B, false);
            }
            lastDotPdfY = pdfY;
        }

        private void disk(float cx, float cy, float r, float red, float green, float blue, boolean filled)
                throws IOException {
            traceCircle(cx, cy, r);
            if (filled) {
                stream.setNonStrokingColor(red, green, blue);
                stream.fill();
            } else {
                stream.setNonStrokingColor(1f, 1f, 1f);
                stream.setStrokingColor(red, green, blue);
                stream.setLineWidth(1.1f);
                stream.fillAndStroke();
            }
        }

        private void pin(float x, float pdfY) throws IOException {
            traceCircle(x + 3.2f, pdfY + 5.4f, 2.5f);
            stream.setNonStrokingColor(INK_R, INK_G, INK_B);
            stream.fill();
            stream.moveTo(x + 1.3f, pdfY + 4.2f);
            stream.lineTo(x + 3.2f, pdfY);
            stream.lineTo(x + 5.1f, pdfY + 4.2f);
            stream.closePath();
            stream.fill();
            traceCircle(x + 3.2f, pdfY + 5.5f, 1f);
            stream.setNonStrokingColor(1f, 1f, 1f);
            stream.fill();
        }

        private void icon(boolean wrench, float x, float pdfY) throws IOException {
            stream.setLineWidth(1.25f);
            stream.setLineCapStyle(1);
            stream.setLineJoinStyle(1);
            if (wrench) {
                stream.setStrokingColor(MUTED_R, MUTED_G, MUTED_B);
                stream.moveTo(x + 1.2f, pdfY + 1.2f);
                stream.lineTo(x + 5.4f, pdfY + 5.4f);
                stream.stroke();
                traceCircle(x + 6.3f, pdfY + 6.5f, 1.7f);
                stream.stroke();
            } else {
                stream.setStrokingColor(CHECK_R, CHECK_G, CHECK_B);
                stream.moveTo(x + 0.8f, pdfY + 3.6f);
                stream.lineTo(x + 3.1f, pdfY + 1.1f);
                stream.lineTo(x + 8f, pdfY + 7.2f);
                stream.stroke();
            }
        }

        private void round(float x, float yTop, float w, float h, float r,
                           float red, float green, float blue, boolean border) throws IOException {
            float y = PAGE_H - yTop - h;
            traceRound(x, y, w, h, r);
            stream.setNonStrokingColor(red, green, blue);
            if (border) {
                stream.setStrokingColor(LINE_R, LINE_G, LINE_B);
                stream.setLineWidth(0.9f);
                stream.fillAndStroke();
            } else {
                stream.fill();
            }
        }

        private void fill(float x, float y, float w, float h, float red, float green, float blue) throws IOException {
            stream.setNonStrokingColor(red, green, blue);
            stream.addRect(x, y, w, h);
            stream.fill();
        }

        private void hairline(float x1, float yTop, float x2) throws IOException {
            stream.setStrokingColor(LINE_R, LINE_G, LINE_B);
            stream.setLineWidth(0.6f);
            float y = PAGE_H - yTop;
            stream.moveTo(x1, y);
            stream.lineTo(x2, y);
            stream.stroke();
        }

        private void vline(float x, float yTop1, float yTop2) throws IOException {
            stream.setStrokingColor(LINE_R, LINE_G, LINE_B);
            stream.setLineWidth(0.7f);
            stream.moveTo(x, PAGE_H - yTop2);
            stream.lineTo(x, PAGE_H - yTop1);
            stream.stroke();
        }

        private void text(String value, PDFont font, float size, float x, float yTop,
                          float red, float green, float blue) throws IOException {
            if (blank(value)) {
                return;
            }
            raw(value, font, size, x, PAGE_H - yTop - ascent(font, size), red, green, blue);
        }

        private void raw(String value, PDFont font, float size, float x, float baseline,
                         float red, float green, float blue) throws IOException {
            if (blank(value)) {
                return;
            }
            stream.setNonStrokingColor(red, green, blue);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, baseline);
            stream.showText(value);
            stream.endText();
        }

        private void fit(PDImageXObject image, float x, float yTop, float w, float h, boolean alignRight)
                throws IOException {
            float scale = Math.min(w / image.getWidth(), h / image.getHeight());
            float dw = image.getWidth() * scale;
            float dh = image.getHeight() * scale;
            float dx = alignRight ? x + w - dw : x + (w - dw) / 2f;
            float dy = yTop + (h - dh) / 2f;
            stream.drawImage(image, dx, PAGE_H - dy - dh, dw, dh);
        }

        private void traceRound(float x, float y, float w, float h, float r) throws IOException {
            float radius = Math.min(r, Math.min(w, h) / 2f);
            float k = 0.55228475f * radius;
            stream.moveTo(x + radius, y);
            stream.lineTo(x + w - radius, y);
            stream.curveTo(x + w - radius + k, y, x + w, y + radius - k, x + w, y + radius);
            stream.lineTo(x + w, y + h - radius);
            stream.curveTo(x + w, y + h - radius + k, x + w - radius + k, y + h, x + w - radius, y + h);
            stream.lineTo(x + radius, y + h);
            stream.curveTo(x + radius - k, y + h, x, y + h - radius + k, x, y + h - radius);
            stream.lineTo(x, y + radius);
            stream.curveTo(x, y + radius - k, x + radius - k, y, x + radius, y);
            stream.closePath();
        }

        private void traceCircle(float cx, float cy, float r) throws IOException {
            float k = 0.55228475f * r;
            stream.moveTo(cx + r, cy);
            stream.curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r);
            stream.curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy);
            stream.curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r);
            stream.curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy);
            stream.closePath();
        }

        private float ascent(PDFont font, float size) {
            if (font.getFontDescriptor() != null && font.getFontDescriptor().getAscent() > 0) {
                return font.getFontDescriptor().getAscent() / 1000f * size;
            }
            return size * 0.8f;
        }

        private float width(String value, PDFont font, float size) throws IOException {
            if (blank(value)) {
                return 0f;
            }
            return font.getStringWidth(value) / 1000f * size;
        }

        private float fitSize(String value, PDFont font, float size, float max) throws IOException {
            if (blank(value) || max <= 0f) {
                return size;
            }
            float w = width(value, font, size);
            if (w <= max) {
                return size;
            }
            return Math.max(6f, size * max / w);
        }

        private List<String> wrap(String value, PDFont font, float size, float max) throws IOException {
            String clean = (value == null ? "" : value).replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
            if (clean.isEmpty()) {
                return List.of();
            }
            List<String> rows = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : clean.split(" ")) {
                if (word.isEmpty()) {
                    continue;
                }
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (width(candidate, font, size) <= max) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                if (!current.isEmpty()) {
                    rows.add(current.toString());
                    current.setLength(0);
                }
                if (width(word, font, size) <= max) {
                    current.append(word);
                    continue;
                }
                String rest = word;
                while (!rest.isEmpty()) {
                    int cut = fitChars(rest, font, size, max);
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
            return rows;
        }

        private int fitChars(String word, PDFont font, float size, float max) throws IOException {
            int low = 1;
            int high = word.length();
            while (low < high) {
                int mid = (low + high + 1) / 2;
                if (width(word.substring(0, mid), font, size) <= max) {
                    low = mid;
                } else {
                    high = mid - 1;
                }
            }
            return low;
        }

        private List<Spot> spots() {
            List<Spot> spots = new ArrayList<>();
            List<DigitalHistoryReport.VisitBlock> visits = report.visits();
            for (int i = 0; i < visits.size(); i++) {
                DigitalHistoryReport.VisitBlock visit = visits.get(i);
                LocalDate date = parseDay(visit.date());
                int km = kilometers(visit.mileage());
                if (date == null || km < 0) {
                    continue;
                }
                spots.add(new Spot(date, km, repair(visit), i));
            }
            spots.sort(Comparator.comparing(Spot::date).thenComparing(Comparator.comparingInt(Spot::order).reversed()));
            return spots;
        }

        private String average(List<Spot> spots) {
            if (spots.size() < 2) {
                return "";
            }
            Spot first = spots.get(0);
            Spot last = spots.get(spots.size() - 1);
            long days = ChronoUnit.DAYS.between(first.date(), last.date());
            if (days <= 0) {
                return "";
            }
            double months = days / 30.4375d;
            long perMonth = Math.round((last.km() - first.km()) / months);
            return "Orta yürüş: ~ " + number(perMonth) + " km / ay";
        }

        private String identity() {
            return join(join(report.brandModel(), report.plate()), report.vin());
        }

        private String centerMoney(String total) throws IOException {
            if (blank(total)) {
                return "—";
            }
            String manat = total.replace(" AZN", " ₼");
            try {
                bold.getStringWidth(manat);
                return manat;
            } catch (IllegalArgumentException ex) {
                return total;
            }
        }

        private PDImageXObject image(String name) throws IOException {
            return PDImageXObject.createFromByteArray(document, resource(name), name);
        }

        private PDImageXObject photo(byte[] data) {
            byte[] printable = pdfImage(data);
            if (printable == null) {
                return null;
            }
            try {
                return PDImageXObject.createFromByteArray(document, printable, "car");
            } catch (IOException | IllegalArgumentException ex) {
                return null;
            }
        }
    }

    /**
     * tr: PDFBox WebP (RIFF) basmaz. Araç fotoğrafı WebP ise JPEG'e çevrilir. Bozuksa null, rapor yine çıkar.
     * en: PDFBox does not print WebP (RIFF). A WebP car photo is turned into JPEG. A broken image is null and the report still prints.
     */
    static byte[] pdfImage(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        if (!webp(data)) {
            return data;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                return null;
            }
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgb.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
            if (!ImageIO.write(rgb, "jpg", jpeg) || jpeg.size() == 0) {
                return null;
            }
            return jpeg.toByteArray();
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private static boolean webp(byte[] data) {
        return data.length >= 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
    }

    private static String dateLine(String day) {
        return blank(day) ? "" : day + " tarixində";
    }

    private static boolean repair(DigitalHistoryReport.VisitBlock visit) {
        String label = visit.label();
        if (label != null && label.toLowerCase(Locale.ROOT).contains("təmir")) {
            return true;
        }
        return visit.inspectionNote() != null && !visit.inspectionNote().isBlank()
                || (visit.inspectionLines() != null && !visit.inspectionLines().isEmpty());
    }

    private static List<Row> rows(DigitalHistoryReport.VisitBlock visit) {
        List<Row> rows = new ArrayList<>();
        add(rows, visit.packageLines(), false);
        add(rows, visit.individualLines(), false);
        if (!blank(visit.inspectionNote())) {
            rows.add(new Row(visit.inspectionNote().trim(), "", true));
        }
        add(rows, visit.inspectionLines(), true);
        add(rows, visit.plainLines(), false);
        return rows;
    }

    private static void add(List<Row> rows, List<String> lines, boolean wrench) {
        if (lines == null) {
            return;
        }
        for (String line : lines) {
            if (blank(line)) {
                continue;
            }
            int cut = line.indexOf(" · ");
            if (cut < 0) {
                rows.add(new Row(line.trim(), "", wrench));
            } else {
                rows.add(new Row(line.substring(0, cut).trim(), line.substring(cut + 3).trim(), wrench));
            }
        }
    }

    private static float rowHeight(Row row) {
        return row.detail().isEmpty() ? 20f : 22f;
    }

    private static String head(DigitalHistoryReport.VisitBlock visit) {
        return join(visit.centerName(), visit.address());
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

    private static String number(long value) {
        String raw = DigitalHistoryFormat.km(value);
        return raw.endsWith(" km") ? raw.substring(0, raw.length() - 3) : raw;
    }

    private static int kilometers(String label) {
        if (label == null) {
            return -1;
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        if (digits.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static LocalDate parseDay(String label) {
        if (blank(label)) {
            return null;
        }
        try {
            return LocalDate.parse(label.trim(), DAY);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record Spot(LocalDate date, int km, boolean repair, int order) {
    }

    private record Row(String name, String detail, boolean wrench) {
    }
}
