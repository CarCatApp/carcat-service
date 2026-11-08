package com.carland.carland_service.digitalhistory;

import org.springframework.web.util.HtmlUtils;

/**
 * tr: Yoxlama səhifəsi. Ağ fon, tünd mətn. Sahib adı və telefon yoxdur.
 * en: Verify page. White background, dark text. No owner name and no phone.
 */
final class DigitalHistoryVerifyHtml {

    private DigitalHistoryVerifyHtml() {
    }

    static String page(DigitalHistoryReport report, String pdfUrl) {
        DigitalHistoryCopy copy = DigitalHistoryCopy.of(report.lang());
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"").append(esc(copy.lang())).append("\"><head><meta charset=\"utf-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        html.append("<title>").append(esc(copy.documentTitle())).append("</title>");
        html.append("<style>");
        html.append("body{margin:0;background:#fff;color:#111214;font:16px/1.45 'Segoe UI',sans-serif;}");
        html.append("main{max-width:720px;margin:0 auto;padding:32px 20px 64px;}");
        html.append("h1{font-size:22px;margin:0 0 8px;} h2{font-size:16px;margin:28px 0 8px;}");
        html.append("p,li{color:#111214;} .muted{color:#5f6570;font-size:14px;}");
        html.append("a{color:#111214;} table{width:100%;border-collapse:collapse;}");
        html.append("td{padding:8px 0;border-bottom:1px solid #e5e7eb;vertical-align:top;} td:last-child{text-align:right;}");
        html.append(".card{border:1px solid #e5e7eb;border-radius:12px;padding:12px 14px;margin:8px 0;}");
        html.append("</style></head><body><main>");
        html.append("<h1>").append(esc(copy.title())).append("</h1>");
        html.append("<p class=\"muted\">№ ").append(esc(report.reportNo()));
        html.append(" · ").append(esc(copy.issued())).append(' ').append(esc(report.issuedAt())).append("</p>");
        html.append("<p><strong>").append(esc(blank(report.brandModel()))).append("</strong><br>");
        html.append(esc(blank(report.plate()))).append(" · ").append(esc(blank(report.vin()))).append("</p>");
        html.append("<p>").append(esc(copy.mileage())).append(": ").append(esc(blank(report.mileage())));
        String mileageOn = copy.mileageOn(report.mileageUpdatedOn());
        if (!mileageOn.isEmpty()) {
            html.append(" <span class=\"muted\">").append(esc(mileageOn)).append("</span>");
        }
        html.append("<br>").append(esc(copy.serviceRecords())).append(": ").append(report.serviceCount());
        if (report.period() != null && !report.period().isBlank()) {
            html.append(" <span class=\"muted\">").append(esc(report.period())).append("</span>");
        }
        html.append("<br>").append(esc(copy.centers())).append(": ").append(report.centerCount());
        if (report.centerCities() != null && !report.centerCities().isBlank()) {
            html.append(" <span class=\"muted\">").append(esc(report.centerCities())).append("</span>");
        }
        html.append("</p>");
        html.append("<h2>").append(esc(copy.technical())).append("</h2><table>");
        if (report.specs() != null) {
            for (DigitalHistoryReport.SpecLine line : report.specs()) {
                html.append("<tr><td>").append(esc(line.label())).append("</td><td>")
                        .append(esc(line.value())).append("</td></tr>");
            }
        }
        html.append("</table>");
        if (report.technicalNote() != null && !report.technicalNote().isBlank()) {
            html.append("<p class=\"muted\">").append(esc(report.technicalNote())).append("</p>");
        }
        html.append("<h2>").append(esc(copy.history())).append("</h2>");
        if (report.visits() == null || report.visits().isEmpty()) {
            html.append("<p>").append(esc(copy.empty())).append("</p>");
        } else {
            for (DigitalHistoryReport.VisitBlock visit : report.visits()) {
                html.append("<div class=\"card\"><strong>")
                        .append(esc(join(visit.date(), visit.mileage(), visit.centerName())))
                        .append("</strong>");
                lines(html, visit.packageLines());
                lines(html, visit.individualLines());
                if (visit.inspectionNote() != null && !visit.inspectionNote().isBlank()) {
                    html.append("<div>").append(esc(visit.inspectionNote())).append("</div>");
                }
                lines(html, visit.inspectionLines());
                lines(html, visit.plainLines());
                html.append("<div class=\"muted\">").append(esc(copy.lineTotal())).append(": ")
                        .append(esc(blank(visit.total()))).append("</div></div>");
            }
        }
        html.append("<p><strong>").append(esc(copy.grandTotal())).append(":</strong> ")
                .append(esc(blank(report.grandTotal()))).append("</p>");
        if (report.centers() != null && !report.centers().isEmpty()) {
            html.append("<h2>").append(esc(copy.centersTitle())).append("</h2>");
            for (DigitalHistoryReport.CenterBlock center : report.centers()) {
                html.append("<div class=\"card\"><strong>").append(esc(blank(center.name()))).append("</strong>");
                if (center.address() != null && !center.address().isBlank()) {
                    html.append("<div>").append(esc(center.address())).append("</div>");
                }
                if (center.phone() != null && !center.phone().isBlank()) {
                    html.append("<div>").append(esc(center.phone())).append("</div>");
                }
                if (center.email() != null && !center.email().isBlank()) {
                    html.append("<div>").append(esc(center.email())).append("</div>");
                }
                html.append("</div>");
            }
        }
        html.append("<p><a href=\"").append(esc(pdfUrl)).append("\">").append(esc(copy.openPdf())).append("</a></p>");
        html.append("<p class=\"muted\">").append(esc(copy.verifyBody())).append("</p>");
        html.append("</main></body></html>");
        return html.toString();
    }

    static String missing() {
        DigitalHistoryCopy copy = DigitalHistoryCopy.of("az");
        return "<!DOCTYPE html><html lang=\"az\"><head><meta charset=\"utf-8\"><title>"
                + esc(copy.missing()) + "</title></head><body style=\"background:#fff;color:#111214;font:16px sans-serif;padding:32px\">"
                + "<p>" + esc(copy.missing()) + "</p></body></html>";
    }

    private static void lines(StringBuilder html, java.util.List<String> lines) {
        if (lines == null) {
            return;
        }
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                html.append("<div>").append(esc(line)).append("</div>");
            }
        }
    }

    private static String join(String date, String mileage, String center) {
        StringBuilder text = new StringBuilder();
        append(text, date);
        append(text, mileage);
        append(text, center);
        return text.toString();
    }

    private static void append(StringBuilder text, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!text.isEmpty()) {
            text.append(" · ");
        }
        text.append(value.trim());
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
