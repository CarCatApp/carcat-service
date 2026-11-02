package com.carland.carland_service.service.clientevent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * tr: Barmaq izi yalnız backend-də. fatal/nonFatal mətn + 5 freym; apiError status + maskalı ünvan.
 *     Sətir nömrəsi hash-ə girmir. apiError path-i yenidən maskalanmır.
 * en: Fingerprints are produced only here. fatal/nonFatal use text plus 5 frames; apiError uses
 *     status plus the already masked path. Line numbers stay out of the hash. apiError paths are not masked again.
 */
public final class ClientEventFingerprint {

    public static final int FRAME_LIMIT = 5;

    private static final Pattern EXCEPTION = Pattern.compile("^([A-Za-z_][A-Za-z0-9_.]*)\\s*:");
    private static final Pattern UUID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern HEX = Pattern.compile("\\b[0-9a-fA-F]{8,}\\b");
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern FRAME_INDEX = Pattern.compile("^#\\d+\\s+");
    private static final Pattern LINE_COL = Pattern.compile(":\\d+:\\d+");
    private static final Pattern LINE_PAREN = Pattern.compile(":\\d+\\)");

    private ClientEventFingerprint() {
    }

    public static String fatal(int version, String eventType, String message, String stack) {
        String type = exceptionType(message);
        String body = normalizeMessage(messageBody(message));
        String frames = String.join("|", frames(stack, FRAME_LIMIT));
        return sha("v" + version + "|" + eventType + "|" + type + "|" + body + "|" + frames);
    }

    public static String api(int version, Integer statusCode, String signal, String endpoint) {
        String key = statusCode != null ? Integer.toString(statusCode) : (signal == null ? "unknown" : signal);
        String path = endpoint == null ? "" : endpoint;
        return sha("v" + version + "|apiError|" + key + "|" + path);
    }

    public static String exceptionType(String message) {
        if (message == null) {
            return "Error";
        }
        var matcher = EXCEPTION.matcher(message.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Error";
    }

    public static String messageBody(String message) {
        if (message == null) {
            return "";
        }
        String trimmed = message.trim();
        var matcher = EXCEPTION.matcher(trimmed);
        if (matcher.find()) {
            return trimmed.substring(matcher.end()).trim();
        }
        return trimmed;
    }

    public static String normalizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String value = UUID.matcher(message).replaceAll("{id}");
        value = HEX.matcher(value).replaceAll("{hex}");
        value = NUMBER.matcher(value).replaceAll("{n}");
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    public static List<String> frames(String stack, int limit) {
        List<String> out = new ArrayList<>();
        if (stack == null || stack.isBlank() || limit <= 0) {
            return out;
        }
        for (String raw : stack.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || (line.startsWith("<") && line.endsWith(">"))) {
                continue;
            }
            line = FRAME_INDEX.matcher(line).replaceFirst("");
            line = LINE_COL.matcher(line).replaceAll("");
            line = LINE_PAREN.matcher(line).replaceAll(")");
            line = line.replaceAll("\\s+", " ").trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.length() > 300) {
                line = line.substring(0, 300);
            }
            out.add(line);
            if (out.size() == limit) {
                break;
            }
        }
        return out;
    }

    public static String apiSignal(String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        String first = message.trim();
        int space = first.indexOf(' ');
        if (space >= 0) {
            first = first.substring(0, space);
        }
        if (!first.matches("[A-Za-z][A-Za-z0-9_]*")) {
            return "unknown";
        }
        return first;
    }

    /**
     * tr: Context ünvanı yoxdursa, mobilin "{tip} {status?} {ünvan}" sətrindən qalan hissə.
     *     Ulduz və path olduğu kimi qalır.
     */
    public static String endpointFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String[] parts = message.trim().split("\\s+");
        int start = 1;
        if (parts.length > 1 && parts[1].matches("\\d{3}")) {
            start = 2;
        }
        if (parts.length <= start) {
            return "";
        }
        StringBuilder path = new StringBuilder();
        for (int i = start; i < parts.length; i++) {
            if (path.length() > 0) {
                path.append(' ');
            }
            path.append(parts[i]);
        }
        return path.toString();
    }

    public static Integer statusFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String[] parts = message.trim().split("\\s+");
        if (parts.length > 1 && parts[1].matches("\\d{3}")) {
            return Integer.parseInt(parts[1]);
        }
        return null;
    }

    private static String sha(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256", ex);
        }
    }
}
