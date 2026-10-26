package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffBookingArrival;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: Hər yeni rezervasiya commit olandan sonra eyni məktub sabit ünvanlara gedir. 10-cu filial əlavə bir qutuya da gedir. Brevo xətası rezervasiyanı geri almır.
 * en: After every new booking commits, the same letter goes to a fixed address list. Branch 10 also goes to one extra inbox. A Brevo failure does not undo the booking.
 */
@Service
@Slf4j
public class BookingCreatedMailService {

    static final String DEFAULT_RECIPIENTS =
            "azadisali@gmail.com,niftalizadeaziz@gmail.com,nemat.mirzayev@gmail.com,Nemat.Mirzayev@carcat.app";
    /** Bu filialın rezervasiyası dörd ünvana əlavə olaraq bu qutuya da gedir. */
    static final long BRANCH_EXTRA_ID = 10L;
    static final String BRANCH_EXTRA_EMAIL = "bayramovanarmin3@gmail.com";

    private final MailService mailService;
    private final List<String> recipients;

    public BookingCreatedMailService(
            MailService mailService,
            @Value("${booking.notify.emails:" + DEFAULT_RECIPIENTS + "}") String emails) {
        this.mailService = mailService;
        this.recipients = parse(emails);
    }

    /**
     * tr: Commitdən sonra çağrılır. Müştəri cavabını Brevo gözləmir.
     * en: Called after commit. The customer response does not wait for Brevo.
     */
    @Async
    public void send(StaffBookingArrival arrival) {
        if (arrival == null || arrival.getBookingId() == null) {
            return;
        }
        List<String> toList = recipientsOf(arrival);
        if (toList.isEmpty()) {
            return;
        }
        String subject = subject(arrival);
        String html = body(arrival);
        for (String to : toList) {
            try {
                mailService.sendPlainMail(to, subject, html);
            } catch (Exception ex) {
                log.warn("BOOKING_CREATED_MAIL_FAIL | id={} ref={} to={} error={}",
                        arrival.getBookingId(), arrival.getRef(), to, ex.getClass().getSimpleName());
            }
        }
    }

    private List<String> recipientsOf(StaffBookingArrival arrival) {
        List<String> to = new ArrayList<>(recipients);
        if (arrival.getBranchId() != null
                && arrival.getBranchId() == BRANCH_EXTRA_ID
                && !to.contains(BRANCH_EXTRA_EMAIL)) {
            to.add(BRANCH_EXTRA_EMAIL);
        }
        return to;
    }

    static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String email = part == null ? "" : part.trim();
            if (!email.isEmpty() && !out.contains(email)) {
                out.add(email);
            }
        }
        return List.copyOf(out);
    }

    static String subject(StaffBookingArrival arrival) {
        String ref = arrival.getRef() == null || arrival.getRef().isBlank() ? "" : " " + arrival.getRef().trim();
        String branch = arrival.getBranchName() == null || arrival.getBranchName().isBlank()
                ? ""
                : " — " + arrival.getBranchName().trim();
        return ("Yeni rezervasiya" + ref + branch).replaceAll("[\\r\\n]+", " ");
    }

    static String body(StaffBookingArrival arrival) {
        StringBuilder html = new StringBuilder();
        html.append("<p><b>Yeni rezervasiya</b></p><p>");
        row(html, "Referans", arrival.getRef());
        row(html, "Partnyor", arrival.getPartnerName());
        row(html, "Filial", arrival.getBranchName());
        row(html, "Tarix", arrival.getDay());
        row(html, "Saat", clock(arrival));
        row(html, "Status", statusLabel(arrival.getStatus()));
        row(html, "Rejim", modeLabel(arrival.getBookingMode()));
        row(html, "Müştəri", arrival.getCustomerName());
        row(html, "Telefon", arrival.getPhone());
        row(html, "Avtomobil", car(arrival));
        row(html, "Nömrə nişanı", arrival.getPlateNumber());
        row(html, "Şassi", arrival.getVin());
        row(html, "Paket", packageLine(arrival));
        row(html, "Xidmətlər", services(arrival));
        row(html, "Qeyd", arrival.getIssue());
        row(html, "Qiymət", price(arrival.getPriceMin(), arrival.getPriceMax()));
        html.append("</p>");
        return html.toString();
    }

    private static void row(StringBuilder html, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        html.append("<b>").append(label).append(":</b> ").append(escape(value)).append("<br/>");
    }

    private static String clock(StaffBookingArrival arrival) {
        String start = arrival.getStart();
        String end = arrival.getEnd();
        if (start == null || start.isBlank()) {
            return end;
        }
        if (end == null || end.isBlank()) {
            return start;
        }
        return start.trim() + "–" + end.trim();
    }

    private static String car(StaffBookingArrival arrival) {
        String brand = arrival.getBrand() == null ? "" : arrival.getBrand().trim();
        String model = arrival.getModel() == null ? "" : arrival.getModel().trim();
        String both = (brand + " " + model).trim();
        return both.isEmpty() ? null : both;
    }

    private static String services(StaffBookingArrival arrival) {
        if (arrival.getServiceNames() == null || arrival.getServiceNames().isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (String name : arrival.getServiceNames()) {
            if (name != null && !name.isBlank()) {
                names.add(name.trim());
            }
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private static String packageLine(StaffBookingArrival arrival) {
        if (arrival.getPackageName() == null || arrival.getPackageName().isBlank()) {
            return null;
        }
        String money = money(arrival.getPackagePrice());
        if (money == null) {
            return arrival.getPackageName().trim();
        }
        return arrival.getPackageName().trim() + " — " + money;
    }

    private static String price(Integer min, Integer max) {
        String left = money(min);
        String right = money(max);
        if (left == null) {
            return right;
        }
        if (right == null || left.equals(right)) {
            return left;
        }
        return left + " – " + right;
    }

    /** Qəpik → 40.00 AZN. Saat yox, pul. */
    static String money(Integer qepik) {
        if (qepik == null) {
            return null;
        }
        int abs = Math.abs(qepik);
        String sign = qepik < 0 ? "-" : "";
        return sign + (abs / 100) + "." + String.format("%02d", abs % 100) + " AZN";
    }

    static String statusLabel(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.trim()) {
            case "pending" -> "Təsdiq gözləyir";
            case "auto_accepted" -> "Avtomatik qəbul";
            default -> status.trim();
        };
    }

    static String modeLabel(String mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode.trim()) {
            case "instant" -> "Ani";
            case "approval" -> "Təsdiqli";
            default -> mode.trim();
        };
    }

    static String escape(String raw) {
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
