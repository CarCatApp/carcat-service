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
     * tr: Doldurulan header ve body ile DigitalFinanceClient.exchange çağırır.
     * en: Calls DigitalFinanceClient.exchange with the header and body the operator typed.
     */
    @PostMapping(value = "/admin/idda-balance", produces = MediaType.TEXT_HTML_VALUE)
    public String query(
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
        Tab selected = Tab.from(tab);
        fillForm(model, selected, requestIdentifier, startDate, endDate, offset, limit);
        clearResult(model);

        Long offsetValue = null;
        Long limitValue = null;
        if (selected != Tab.BALANCE) {
            try {
                offsetValue = parseWhole(offset);
            } catch (NumberFormatException ex) {
                model.addAttribute("error", "Offset tam sayı olmalı.");
                return "idda-balance";
            }
            try {
                limitValue = parseWhole(limit);
            } catch (NumberFormatException ex) {
                model.addAttribute("error", "Limit tam sayı olmalı.");
                return "idda-balance";
            }
        }

        String requestId = blankToNull(requestIdentifier);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        model.addAttribute("requestIdentifier", requestId);

        byte[] body = null;
        if (selected != Tab.BALANCE) {
            ObjectNode node = MAPPER.createObjectNode();
            putText(node, "StartDate", startDate);
            putText(node, "EndDate", endDate);
            if (offsetValue != null) {
                node.put("Offset", offsetValue);
            }
            if (limitValue != null) {
                node.put("Limit", limitValue);
            }
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
        return "idda-balance";
    }

    private static void fillForm(Model model, Tab tab, String requestIdentifier, String startDate,
                                 String endDate, String offset, String limit) {
        model.addAttribute("tab", tab.key());
        model.addAttribute("upstream", tab.label());
        model.addAttribute("requestIdentifier", empty(requestIdentifier));
        model.addAttribute("startDate", empty(startDate));
        model.addAttribute("endDate", empty(endDate));
        model.addAttribute("offset", empty(offset));
        model.addAttribute("limit", empty(limit));
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

    private static void putText(ObjectNode node, String field, String raw) {
        String value = blankToNull(raw);
        if (value != null) {
            node.put(field, value);
        }
    }

    private static Long parseWhole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.matches("-?\\d+")) {
            throw new NumberFormatException(trimmed);
        }
        return Long.parseLong(trimmed);
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
