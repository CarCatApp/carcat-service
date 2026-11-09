package com.carland.carland_service.digitalhistory;

import org.springframework.web.util.HtmlUtils;

/**
 * tr: Yoxlama səhifəsi. Hesabat datası yoxdur: yalnız PDF keçidi və xəbərdarlıq.
 * en: Verify page. No report data: only the PDF link and the disclaimer.
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
        html.append("a{color:#111214;font-weight:600;} .muted{color:#5f6570;font-size:14px;}");
        html.append("</style></head><body><main>");
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

    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
