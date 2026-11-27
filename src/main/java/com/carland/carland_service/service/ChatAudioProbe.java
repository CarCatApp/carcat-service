package com.carland.carland_service.service;

import com.carland.carland_service.exceptions.MissingFieldException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * tr: AAC .m4a doğrulaması. Uzantı ve Content-Type kullanılmaz; ffprobe codec, container ve süreyi okur.
 * en: AAC .m4a check. Extension and Content-Type are ignored; ffprobe reads codec, container and duration.
 */
public final class ChatAudioProbe {

    public static final long MAX_BYTES = 3L * 1024 * 1024;
    public static final long MAX_DURATION_MS = 5L * 60 * 1000;
    /** Client and ffprobe within this window count as the same length. */
    static final long DURATION_TOLERANCE_MS = 1000;

    private static final ObjectMapper JSON = new ObjectMapper();

    private ChatAudioProbe() {
    }

    public record Result(long durationMs) {
    }

    /** Bytes stored after validation. Browser Opus/WebM is converted to AAC .m4a. */
    public record Encoded(byte[] bytes, long durationMs) {
    }

    public record Slice(int status, byte[] body, String contentRange) {
    }

    public static Result inspect(byte[] data, String ffprobeCommand) {
        requireSize(data);
        return readReport(probe(data, ffprobeCommand));
    }

    /**
     * tr: Telefondan gelen AAC .m4a olduğu gibi kalır. Tarayıcı kaydı (Opus/WebM) ffmpeg ile AAC .m4a olur.
     * en: A phone AAC .m4a is kept. A browser recording (Opus/WebM) is converted to AAC .m4a with ffmpeg.
     */
    public static Encoded normalize(byte[] data, String ffprobeCommand, String ffmpegCommand) {
        requireSize(data);
        JsonNode report = probe(data, ffprobeCommand);
        Long duration = measuredDuration(report);
        if (duration != null && duration > MAX_DURATION_MS) {
            throw new MissingFieldException("audioTooLong");
        }
        try {
            Result ready = readReport(report);
            return new Encoded(data, ready.durationMs());
        } catch (MissingFieldException ex) {
            if (!"unsupportedMedia".equals(ex.getMessage())) {
                throw ex;
            }
        }
        if (!convertibleVoice(report)) {
            throw new MissingFieldException("unsupportedMedia");
        }
        byte[] m4a = transcode(data, ffmpegCommand);
        Result checked = inspect(m4a, ffprobeCommand);
        return new Encoded(m4a, checked.durationMs());
    }

    static boolean convertibleVoice(JsonNode root) {
        JsonNode streams = root == null ? null : root.path("streams");
        if (streams == null || !streams.isArray()) {
            return false;
        }
        boolean audio = false;
        for (JsonNode stream : streams) {
            String type = stream.path("codec_type").asText("").toLowerCase(Locale.ROOT);
            if ("video".equals(type) && !attachedPicture(stream)) {
                return false;
            }
            if ("audio".equals(type)) {
                audio = true;
            }
        }
        return audio;
    }

    private static void requireSize(byte[] data) {
        if (data == null || data.length == 0) {
            throw new MissingFieldException("unsupportedMedia");
        }
        if (data.length > MAX_BYTES) {
            throw new MissingFieldException("payloadTooLarge");
        }
    }

    private static JsonNode probe(byte[] data, String ffprobeCommand) {
        String command = ffprobeCommand == null || ffprobeCommand.isBlank() ? "ffprobe" : ffprobeCommand.trim();
        Path temp = null;
        try {
            temp = Files.createTempFile("chat-audio-", ".bin");
            Files.write(temp, data);
            byte[] output = run(command, 10, "-v", "error", "-print_format", "json", "-show_format", "-show_streams",
                    temp.toAbsolutePath().toString());
            return JSON.readTree(output);
        } catch (MissingFieldException | IllegalStateException ex) {
            throw ex;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new MissingFieldException("unsupportedMedia");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("ffprobe unavailable", ex);
        } catch (Exception ex) {
            throw new MissingFieldException("unsupportedMedia");
        } finally {
            deleteTemp(temp);
        }
    }

    private static byte[] transcode(byte[] data, String ffmpegCommand) {
        String command = ffmpegCommand == null || ffmpegCommand.isBlank() ? "ffmpeg" : ffmpegCommand.trim();
        Path in = null;
        Path out = null;
        try {
            in = Files.createTempFile("chat-audio-in-", ".bin");
            out = Files.createTempFile("chat-audio-out-", ".m4a");
            Files.write(in, data);
            run(command, 20, "-y", "-i", in.toAbsolutePath().toString(), "-vn", "-ac", "1", "-c:a", "aac", "-b:a", "64k",
                    "-movflags", "+faststart", out.toAbsolutePath().toString());
            return Files.readAllBytes(out);
        } catch (MissingFieldException | IllegalStateException ex) {
            throw ex;
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("ffmpeg unavailable", ex);
        } catch (Exception ex) {
            throw new MissingFieldException("unsupportedMedia");
        } finally {
            deleteTemp(in);
            deleteTemp(out);
        }
    }

    private static byte[] run(String command, int timeoutSeconds, String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = command;
        System.arraycopy(args, 0, cmd, 1, args.length);
        Process process = new ProcessBuilder(cmd)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> {
            try (InputStream in = process.getInputStream()) {
                return in.readAllBytes();
            } catch (Exception ex) {
                return new byte[0];
            }
        });
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            output.cancel(true);
            throw new IllegalStateException(command + " timed out");
        }
        if (process.exitValue() != 0) {
            throw new MissingFieldException("unsupportedMedia");
        }
        return output.get(2, TimeUnit.SECONDS);
    }

    private static void deleteTemp(Path temp) {
        if (temp == null) {
            return;
        }
        try {
            Files.deleteIfExists(temp);
        } catch (Exception ignored) {
            // temp file is the only leftover
        }
    }

    private static Long measuredDuration(JsonNode root) {
        if (root == null) {
            return null;
        }
        Long duration = durationMs(root.path("format").path("duration"));
        if (duration != null) {
            return duration;
        }
        JsonNode streams = root.path("streams");
        if (!streams.isArray()) {
            return null;
        }
        for (JsonNode stream : streams) {
            if ("audio".equalsIgnoreCase(stream.path("codec_type").asText(""))) {
                duration = durationMs(stream.path("duration"));
                if (duration != null) {
                    return duration;
                }
            }
        }
        return null;
    }

    static Result readReport(JsonNode root) {
        if (root == null || !m4aContainer(root.path("format").path("format_name").asText(""))) {
            throw new MissingFieldException("unsupportedMedia");
        }
        boolean aac = false;
        JsonNode streams = root.path("streams");
        if (!streams.isArray()) {
            throw new MissingFieldException("unsupportedMedia");
        }
        for (JsonNode stream : streams) {
            String type = stream.path("codec_type").asText("").toLowerCase(Locale.ROOT);
            if ("video".equals(type) && !attachedPicture(stream)) {
                throw new MissingFieldException("unsupportedMedia");
            }
            if ("audio".equals(type)) {
                String codec = stream.path("codec_name").asText("").toLowerCase(Locale.ROOT);
                if (!"aac".equals(codec)) {
                    throw new MissingFieldException("unsupportedMedia");
                }
                aac = true;
            }
        }
        if (!aac) {
            throw new MissingFieldException("unsupportedMedia");
        }
        Long duration = durationMs(root.path("format").path("duration"));
        if (duration == null) {
            for (JsonNode stream : streams) {
                if ("audio".equals(stream.path("codec_type").asText(""))) {
                    duration = durationMs(stream.path("duration"));
                    if (duration != null) {
                        break;
                    }
                }
            }
        }
        if (duration == null || duration <= 0 || duration > MAX_DURATION_MS) {
            throw new MissingFieldException(duration != null && duration > MAX_DURATION_MS
                    ? "audioTooLong" : "unsupportedMedia");
        }
        return new Result(duration);
    }

    /** Client value stands when it is within {@link #DURATION_TOLERANCE_MS} of ffprobe. Otherwise ffprobe wins. */
    public static long resolveDuration(Long clientMs, Long probeMs) {
        if (probeMs == null) {
            throw new MissingFieldException("unsupportedMedia");
        }
        if (clientMs == null || clientMs < 0) {
            return probeMs;
        }
        if (Math.abs(clientMs - probeMs) > DURATION_TOLERANCE_MS) {
            return probeMs;
        }
        return clientMs;
    }

    /**
     * One byte range. No header → 200. A satisfiable {@code Range} → 206. Anything else → 416.
     */
    public static Slice slice(byte[] data, String rangeHeader) {
        int total = data == null ? 0 : data.length;
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return new Slice(200, data, null);
        }
        String value = rangeHeader.trim();
        if (total == 0 || value.length() < 7 || !value.regionMatches(true, 0, "bytes=", 0, 6)) {
            return unsatisfiable(total);
        }
        String spec = value.substring(6).trim();
        int comma = spec.indexOf(',');
        if (comma >= 0) {
            spec = spec.substring(0, comma).trim();
        }
        int dash = spec.indexOf('-');
        if (dash < 0) {
            return unsatisfiable(total);
        }
        String left = spec.substring(0, dash).trim();
        String right = spec.substring(dash + 1).trim();
        long start;
        long end;
        try {
            if (left.isEmpty()) {
                if (right.isEmpty()) {
                    return unsatisfiable(total);
                }
                long suffix = Long.parseLong(right);
                if (suffix <= 0) {
                    return unsatisfiable(total);
                }
                start = Math.max(0, total - suffix);
                end = total - 1L;
            } else {
                start = Long.parseLong(left);
                end = right.isEmpty() ? total - 1L : Long.parseLong(right);
            }
        } catch (NumberFormatException ex) {
            return unsatisfiable(total);
        }
        if (start < 0 || start >= total || end < start) {
            return unsatisfiable(total);
        }
        if (end >= total) {
            end = total - 1L;
        }
        int from = (int) start;
        int to = (int) end;
        byte[] body = Arrays.copyOfRange(data, from, to + 1);
        return new Slice(206, body, "bytes " + from + "-" + to + "/" + total);
    }

    private static Slice unsatisfiable(int total) {
        return new Slice(416, new byte[0], "bytes */" + total);
    }

    private static boolean m4aContainer(String formatName) {
        for (String token : formatName.toLowerCase(Locale.ROOT).split(",")) {
            String name = token.trim();
            if ("mp4".equals(name) || "m4a".equals(name) || "mov".equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean attachedPicture(JsonNode stream) {
        JsonNode value = stream.path("disposition").path("attached_pic");
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            return value.asInt() == 1;
        }
        String text = value.asText("");
        return "1".equals(text) || "true".equalsIgnoreCase(text);
    }

    private static Long durationMs(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText("").trim();
        if (text.isEmpty() || "N/A".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            double seconds = Double.parseDouble(text);
            if (!Double.isFinite(seconds) || seconds < 0) {
                return null;
            }
            return Math.round(seconds * 1000d);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
