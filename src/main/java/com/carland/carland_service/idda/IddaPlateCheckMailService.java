package com.carland.carland_service.idda;

import com.carland.carland_service.service.MailService;
import com.carland.carland_service.util.PlateNumbers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * tr: Mutlu yol Code 0 ve boş mesajdır, mektup açmaz. Diğer her canlı IDDA cevabı beş adrese gider.
 *     Code 0 + Məlumat tapılmadı da gider. Limit ve teknik hata mektup açmaz.
 *     Brevo hatası plaka sorgusunu geri almaz. Saat Asia/Baku, 24 saat.
 * en: The happy path is Code 0 with a blank message and sends nothing. Every other live IDDA answer goes to five addresses.
 *     Code 0 with Məlumat tapılmadı is included. The cap and a technical failure send nothing.
 *     A Brevo failure does not undo the plate check. The clock is Asia/Baku, 24-hour.
 */
@Service
@Slf4j
public class IddaPlateCheckMailService {

    static final List<String> RECIPIENTS = List.of(
            "azadisali@gmail.com",
            "niftalizadeaziz@gmail.com",
            "nemat.mirzayev@gmail.com",
            "Nemat.Mirzayev@carcat.app",
            "besirov.business@gmail.com"
    );

    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final MailService mailService;

    public IddaPlateCheckMailService(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * tr: Committen sonra çağrılır. Mutlu yol mektup açmaz.
     * en: Called after commit. The happy path sends nothing.
     */
    @Async
    public void send(IddaPlateCheck row) {
        if (!alerts(row)) {
            return;
        }
        String subject = subject(row);
        String html = body(row);
        for (String to : RECIPIENTS) {
            try {
                mailService.sendPlainMail(to, subject, html);
            } catch (Exception ex) {
                log.warn("IDDA_PLATE_MAIL_FAIL | code={} plateKey={} to={} error={}",
                        row.getStatusCode(), row.getPlateKey(), to, ex.getClass().getSimpleName());
            }
        }
    }

    /**
     * tr: Mutlu yol Code 0 ve boş IDDA mesajıdır. Başka her kod ve dolu mesaj mektup açar.
     * en: The happy path is Code 0 and a blank IDDA message. Every other code and a filled message alerts.
     */
    static boolean alerts(IddaPlateCheck row) {
        if (row == null || row.getStatusCode() == null) {
            return false;
        }
        return row.getStatusCode() != 0 || (row.getStatusMessage() != null && !row.getStatusMessage().isBlank());
    }

    static String subject(IddaPlateCheck row) {
        String name = row.getStatusName() == null || row.getStatusName().isBlank()
                ? ""
                : " " + row.getStatusName().trim();
        String plate = PlateNumbers.format(row.getPlateNumber());
        String platePart = plate == null || plate.isBlank() ? "" : " — " + plate;
        return ("IDDA " + row.getStatusCode() + name + platePart).replaceAll("[\\r\\n]+", " ").trim();
    }

    static String body(IddaPlateCheck row) {
        StringBuilder html = new StringBuilder();
        html.append("<p><b>IDDA cavabı</b></p><p>");
        always(html, "Kod", row.getStatusCode() == null ? "" : Integer.toString(row.getStatusCode()));
        always(html, "Status", row.getStatusName());
        always(html, "Mesaj", row.getStatusMessage());
        row(html, "Nişan", PlateNumbers.format(row.getPlateNumber()));
        row(html, "Vaxt", clock(row.getQueriedAt()));
        row(html, "Sorğu", row.getRequestIdentifier());
        row(html, "İstifadəçi", row.getQuerierUserId() == null ? null : Long.toString(row.getQuerierUserId()));
        row(html, "Haq", row.isCountsTowardLimit() ? "bəli" : "xeyr");
        html.append("</p>");
        return html.toString();
    }

    static String clock(LocalDateTime queriedAt) {
        if (queriedAt == null) {
            return null;
        }
        return queriedAt.atZone(ZoneId.systemDefault()).withZoneSameInstant(BAKU).format(CLOCK);
    }

    private static void always(StringBuilder html, String label, String value) {
        html.append("<b>").append(label).append(":</b> ").append(escape(value == null ? "" : value)).append("<br/>");
    }

    private static void row(StringBuilder html, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        html.append("<b>").append(label).append(":</b> ").append(escape(value)).append("<br/>");
    }

    static String escape(String raw) {
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
