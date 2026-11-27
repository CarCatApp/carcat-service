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
 * tr: Code 2 dışında her canlı IDDA cevabı dört adrese gider. Code 2, limit ve teknik hata mektup açmaz.
 *     Brevo hatası plaka sorgusunu geri almaz. Saat Asia/Baku, 24 saat.
 * en: Every live IDDA answer except Code 2 goes to four addresses. Code 2, the cap and a technical failure send nothing.
 *     A Brevo failure does not undo the plate check. The clock is Asia/Baku, 24-hour.
 */
@Service
@Slf4j
public class IddaPlateCheckMailService {

    static final List<String> RECIPIENTS = List.of(
            "azadisali@gmail.com",
            "niftalizadeaziz@gmail.com",
            "nemat.mirzayev@gmail.com",
            "Nemat.Mirzayev@carcat.app"
    );

    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final MailService mailService;

    public IddaPlateCheckMailService(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * tr: Committen sonra çağrılır. Code 2 buraya gelse de mektup gitmez.
     * en: Called after commit. Code 2 still sends nothing if it reaches here.
     */
    @Async
    public void send(IddaPlateCheck row) {
        if (row == null || row.getStatusCode() == null || row.getStatusCode() == 2) {
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
        row(html, "Kod", row.getStatusCode() == null ? null : Integer.toString(row.getStatusCode()));
        row(html, "Status", row.getStatusName());
        row(html, "Mesaj", row.getStatusMessage());
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
