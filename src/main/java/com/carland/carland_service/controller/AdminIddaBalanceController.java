package com.carland.carland_service.controller;

import com.carland.carland_service.digitalfinance.DigitalFinanceClient;
import com.carland.carland_service.security.AdminAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * tr: Admin iddaBalanceInfo. Balance, expense ve payment aynı DigitalFinanceClient çağrısını kullanır.
 *     ApiKey forma yazılmaz. Sekme açmak upstream çağırmaz; Sorgula gönderir.
 * en: Admin iddaBalanceInfo. Balance, expense and payment use the same DigitalFinanceClient call.
 *     ApiKey is not a form field. Opening a tab does not call upstream; Sorgula does.
 */
@Slf4j
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminIddaBalanceController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";
    private static final long DEFAULT_PAGE_SIZE = 50L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AdminAccessService adminAccessService;
    private final DigitalFinanceClient digitalFinanceClient;

    /**
     * tr: Formu gösterir. Upstream çağrısı yok.
     * en: Shows the form. No upstream call.
     */
    @GetMapping(value = "/admin/idda-balance", produces = MediaType.TEXT_HTML_VALUE)
    public String page(
            @RequestParam(name = "tab", required = false) String tab,
            @RequestParam(name = "RequestIdentifier", required = false) String requestIdentifier,
            @RequestParam(name = "StartDate", required = false) String startDate,
            @RequestParam(name = "EndDate", required = false) String endDate,
            @RequestParam(name = "Offset", required = false) String offset,
            @RequestParam(name = "Limit", required = false) String limit,
            HttpServletRequest request,
            Model model
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        fillForm(model, Tab.from(tab), requestIdentifier, startDate, endDate, offset, limit);
        clearResult(model);
        return "idda-balance";
    }

    /**
     * tr: Günü 00:00:00 / 23:59:59 yapar. Sayfa boyutunu ve Offset'i hesaplayıp DigitalFinanceClient.exchange çağırır.
     * en: Expands a day to 00:00:00 / 23:59:59, computes page size and Offset, then calls DigitalFinanceClient.exchange.
     */
    @PostMapping(value = "/admin/idda-balance", produces = MediaType.TEXT_HTML_VALUE)
    public String query(
            @RequestParam(name = "tab", required = false) String tab,
            @RequestParam(name = "RequestIdentifier", required = false) String requestIdentifier,
            @RequestParam(name = "StartDate", required = false) String startDate,
            @RequestParam(name = "EndDate", required = false) String endDate,
            @RequestParam(name = "Offset", required = false) String offset,
            @RequestParam(name = "Limit", required = false) String limit,
            @RequestParam(name = "nav", required = false) String nav,
            HttpServletRequest request,
            Model model
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        Tab selected = Tab.from(tab);
        clearResult(model);

        long pageOffset = 0L;
        long pageSize = DEFAULT_PAGE_SIZE;
        LocalDate startDay = null;
        LocalDate endDay = null;
        if (selected != Tab.BALANCE) {
            try {
                startDay = parseDay(startDate);
            } catch (DateTimeParseException ex) {
                fillForm(model, selected, requestIdentifier, startDate, endDate, offset, limit);
                model.addAttribute("error", "Başlangıç günü yyyy-MM-dd olmalı.");
                return "idda-balance";
            }
            try {
                endDay = parseDay(endDate);
            } catch (DateTimeParseException ex) {
                fillForm(model, selected, requestIdentifier, startDate, endDate, offset, limit);
                model.addAttribute("error", "Bitiş günü yyyy-MM-dd olmalı.");
                return "idda-balance";
            }
            if (startDay != null && endDay != null && endDay.isBefore(startDay)) {
                fillForm(model, selected, requestIdentifier, startDate, endDate, offset, limit);
                model.addAttribute("error", "Bitiş, başlangıçtan önce olamaz.");
                return "idda-balance";
            }
            try {
                pageSize = parsePageSize(limit);
            } catch (NumberFormatException ex) {
                fillForm(model, selected, requestIdentifier, startDate, endDate, offset, limit);
                model.addAttribute("error", "Sayfa başı 20, 50 veya 100 olmalı.");
                return "idda-balance";
            }
            boolean paging = "next".equals(nav) || "prev".equals(nav);
            if (paging) {
                try {
                    pageOffset = parseOffset(offset);
                } catch (NumberFormatException ex) {
                    fillForm(model, selected, requestIdentifier, startDate, endDate, offset, limit);
                    model.addAttribute("error", "Offset tam sayı olmalı.");
                    return "idda-balance";
                }
                if ("next".equals(nav)) {
                    if (pageOffset > Long.MAX_VALUE - pageSize) {
                        fillForm(model, selected, requestIdentifier, startDate, endDate, offset, limit);
                        model.addAttribute("error", "Sonraki sayfa yok.");
                        return "idda-balance";
                    }
                    pageOffset = pageOffset + pageSize;
                } else {
                    pageOffset = pageOffset < pageSize ? 0L : pageOffset - pageSize;
                }
            }
        }

        fillForm(
                model,
                selected,
                requestIdentifier,
                startDay == null ? startDate : startDay.toString(),
                endDay == null ? endDate : endDay.toString(),
                selected == Tab.BALANCE ? offset : Long.toString(pageOffset),
                selected == Tab.BALANCE ? limit : Long.toString(pageSize)
        );

        String requestId = blankToNull(requestIdentifier);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        model.addAttribute("requestIdentifier", requestId);

        byte[] body = null;
        if (selected != Tab.BALANCE) {
            ObjectNode node = MAPPER.createObjectNode();
            if (startDay != null) {
                node.put("StartDate", startDay + " 00:00:00");
            }
            if (endDay != null) {
                node.put("EndDate", endDay + " 23:59:59");
            }
            node.put("Offset", pageOffset);
            node.put("Limit", pageSize);
            try {
                body = MAPPER.writeValueAsBytes(node);
                model.addAttribute("sentBody", MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node));
            } catch (JsonProcessingException ex) {
                model.addAttribute("error", "İstek gövdesi yazılamadı.");
                return "idda-balance";
            }
        }

        DigitalFinanceClient.Upstream upstream;
        try {
            upstream = digitalFinanceClient.exchange(selected.method(), selected.path(), body, requestId, null);
        } catch (IOException | IllegalStateException ex) {
            log.warn("Digital Finance admin {} {} failed: {}", selected.method(), selected.path(), ex.getClass().getSimpleName());
            model.addAttribute("error", callError(ex));
            return "idda-balance";
        }
        applyUpstream(model, upstream);
        if (selected != Tab.BALANCE) {
            bindPager(model, pageOffset, pageSize);
        }
        return "idda-balance";
    }

    private static void fillForm(Model model, Tab tab, String requestIdentifier, String startDate,
                                 String endDate, String offset, String limit) {
        model.addAttribute("tab", tab.key());
        model.addAttribute("upstream", tab.label());
        model.addAttribute("requestIdentifier", empty(requestIdentifier));
        model.addAttribute("startDate", displayDay(startDate));
        model.addAttribute("endDate", displayDay(endDate));
        model.addAttribute("offset", displayOffset(offset));
        model.addAttribute("limit", displayLimit(limit));
    }

    private static void clearResult(Model model) {
        model.addAttribute("queried", false);
        model.addAttribute("error", null);
        model.addAttribute("httpStatus", null);
        model.addAttribute("responseRequestId", null);
        model.addAttribute("statusName", null);
        model.addAttribute("statusCode", null);
        model.addAttribute("statusMessage", null);
        model.addAttribute("balance", null);
        model.addAttribute("balanceManat", null);
        model.addAttribute("count", null);
        model.addAttribute("expenseListed", false);
        model.addAttribute("paymentListed", false);
        model.addAttribute("expenseRows", List.of());
        model.addAttribute("paymentRows", List.of());
        model.addAttribute("rawJson", null);
        model.addAttribute("sentBody", null);
        model.addAttribute("showPager", false);
        model.addAttribute("hasPrev", false);
        model.addAttribute("hasNext", false);
        model.addAttribute("pageLabel", "");
    }

    private static void applyUpstream(Model model, DigitalFinanceClient.Upstream upstream) {
        model.addAttribute("queried", true);
        model.addAttribute("httpStatus", upstream.statusCode());
        byte[] bytes = upstream.body() == null ? new byte[0] : upstream.body();
        if (bytes.length == 0) {
            model.addAttribute("rawJson", "");
            return;
        }
        try {
            JsonNode root = MAPPER.readTree(bytes);
            model.addAttribute("rawJson", MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
            model.addAttribute("responseRequestId", scalar(root.get("RequestIdentifier")));
            JsonNode status = root.get("Status");
            if (status != null && !status.isNull()) {
                model.addAttribute("statusName", scalar(status.get("Name")));
                model.addAttribute("statusCode", scalar(status.get("Code")));
                model.addAttribute("statusMessage", scalar(status.get("Message")));
            }
            JsonNode response = root.get("Response");
            if (response == null || response.isNull() || !response.isObject()) {
                return;
            }
            if (response.has("Balance") && !response.get("Balance").isNull()) {
                String balance = scalar(response.get("Balance"));
                model.addAttribute("balance", balance);
                model.addAttribute("balanceManat", manat(balance));
            }
            if (response.has("Count") && !response.get("Count").isNull()) {
                model.addAttribute("count", scalar(response.get("Count")));
            }
            JsonNode expenses = response.get("Expenses");
            if (expenses != null && expenses.isArray()) {
                List<ExpenseLine> rows = new ArrayList<>();
                for (JsonNode item : expenses) {
                    String amount = scalar(item.get("Amount"));
                    rows.add(new ExpenseLine(
                            scalar(item.get("Value")),
                            scalar(item.get("Date")),
                            scalar(item.get("Service")),
                            amount,
                            manat(amount),
                            scalar(item.get("RequestIdentifier")),
                            scalar(item.get("IpAddress"))
                    ));
                }
                model.addAttribute("expenseListed", true);
                model.addAttribute("expenseRows", rows);
            }
            JsonNode payments = response.get("Payments");
            if (payments != null && payments.isArray()) {
                List<PaymentLine> rows = new ArrayList<>();
                for (JsonNode item : payments) {
                    rows.add(new PaymentLine(scalar(item.get("Date")), scalar(item.get("Amount"))));
                }
                model.addAttribute("paymentListed", true);
                model.addAttribute("paymentRows", rows);
            }
        } catch (Exception ex) {
            model.addAttribute("rawJson", new String(bytes, StandardCharsets.UTF_8));
        }
    }

    private static void bindPager(Model model, long offset, long pageSize) {
        Long count = countOrNull(model.getAttribute("count"));
        if (count == null) {
            return;
        }
        long from = 0L;
        long to = 0L;
        if (count > 0 && offset < count) {
            from = offset + 1;
            to = Math.min(offset + pageSize, count);
        }
        String label = count == 0 || from == 0
                ? "0 / " + count
                : from + "–" + to + " / " + count;
        model.addAttribute("showPager", true);
        model.addAttribute("hasPrev", offset > 0);
        model.addAttribute("hasNext", offset + pageSize < count);
        model.addAttribute("pageLabel", label);
    }

    private static Long countOrNull(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.toString().trim();
        if (!text.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * tr: Boş gün yok sayılır. yyyy-MM-dd veya yyyy-MM-dd HH:mm:ss kabul edilir; saat kısmı atılır.
     * en: A blank day is absent. Accepts yyyy-MM-dd or yyyy-MM-dd HH:mm:ss and drops the clock time.
     */
    private static LocalDate parseDay(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        String day = trimmed.length() >= 10 ? trimmed.substring(0, 10) : trimmed;
        if (!day.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new DateTimeParseException("day", trimmed, 0);
        }
        if (trimmed.length() > 10 && !trimmed.substring(10).matches(" \\d{2}:\\d{2}:\\d{2}")) {
            throw new DateTimeParseException("time", trimmed, 10);
        }
        return LocalDate.parse(day);
    }

    private static long parsePageSize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_PAGE_SIZE;
        }
        String trimmed = raw.trim();
        if (!trimmed.matches("\\d+")) {
            throw new NumberFormatException(trimmed);
        }
        long value = Long.parseLong(trimmed);
        if (value != 20L && value != 50L && value != 100L) {
            throw new NumberFormatException(trimmed);
        }
        return value;
    }

    private static long parseOffset(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        String trimmed = raw.trim();
        if (!trimmed.matches("\\d+")) {
            throw new NumberFormatException(trimmed);
        }
        return Long.parseLong(trimmed);
    }

    private static String displayDay(String raw) {
        try {
            LocalDate day = parseDay(raw);
            return day == null ? "" : day.toString();
        } catch (DateTimeParseException ex) {
            return "";
        }
    }

    private static String displayOffset(String raw) {
        try {
            return Long.toString(parseOffset(raw));
        } catch (NumberFormatException ex) {
            return "0";
        }
    }

    private static String displayLimit(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                return "";
            }
            return Long.toString(parsePageSize(raw));
        } catch (NumberFormatException ex) {
            return "";
        }
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

    private static String scalar(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue().toPlainString();
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return node.asText();
    }

    private static String callError(Exception ex) {
        if (ex instanceof IllegalStateException && ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }
        return "Digital Finance çağrısı tamamlanmadı.";
    }

    private static String empty(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static String blankToNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private enum Tab {
        EXPENSE, PAYMENT, BALANCE;

        static Tab from(String raw) {
            if (raw == null) {
                return EXPENSE;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "payment" -> PAYMENT;
                case "balance" -> BALANCE;
                default -> EXPENSE;
            };
        }

        String key() {
            return switch (this) {
                case EXPENSE -> "expense";
                case PAYMENT -> "payment";
                case BALANCE -> "balance";
            };
        }

        String method() {
            return this == BALANCE ? "GET" : "POST";
        }

        String path() {
            return switch (this) {
                case EXPENSE -> "/api/v1/Info/Expenses";
                case PAYMENT -> "/api/v1/Info/Payments";
                case BALANCE -> "/api/v1/Info/Balance";
            };
        }

        String label() {
            return method() + " " + path();
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static final class ExpenseLine {
        private final String value;
        private final String date;
        private final String service;
        private final String amount;
        private final String amountManat;
        private final String requestIdentifier;
        private final String ipAddress;
    }

    @Getter
    @RequiredArgsConstructor
    public static final class PaymentLine {
        private final String date;
        private final String amount;
    }
}
