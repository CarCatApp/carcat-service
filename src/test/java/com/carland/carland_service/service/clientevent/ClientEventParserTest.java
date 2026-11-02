package com.carland.carland_service.service.clientevent;

import com.carland.carland_service.config.ClientEventProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientEventParserTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ClientEventProperties limits = new ClientEventProperties();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T06:20:00Z"), ZoneOffset.UTC);

    @Test
    void keepsAGuestFatalAndDropsTheBrokenSiblingFields() throws Exception {
        var node = mapper.readTree("""
                {
                  "eventId": "11111111-1111-4111-8111-111111111111",
                  "type": "fatal",
                  "timestamp": "2026-11-02T06:15:00.000Z",
                  "message": "FormatException: boom",
                  "stackTrace": "#0 Foo.bar (package:carcat/src/a.dart:12:3)",
                  "sessionId": "22222222-2222-4222-8222-222222222222",
                  "installId": "33333333-3333-4333-8333-333333333333",
                  "app": {"version": "1.4.0", "buildNumber": "42", "packageName": "com.nematmirzayev.carcat"},
                  "os": {"name": "ios", "version": "18.0"},
                  "environment": "release",
                  "network": "WIFI"
                }
                """);
        ClientEventParser.Parsed parsed = ClientEventParser.parse(node, limits, clock);
        assertEquals("fatal", parsed.eventType());
        assertNull(parsed.userId());
        assertEquals("wifi", parsed.network());
        assertTrue(parsed.stackTrace().contains("Foo.bar"));
    }

    @Test
    void dropsDebugAndATooLongInstallId() throws Exception {
        assertNull(ClientEventParser.parse(mapper.readTree(base("environment", "debug")), limits, clock));
        assertNull(ClientEventParser.parse(mapper.readTree(base("installId", "x".repeat(65))), limits, clock));
        assertNull(ClientEventParser.parse(mapper.readTree(base("message", "  ")), limits, clock));
    }

    @Test
    void clipsMessageAndStackAndDropsApiStack() throws Exception {
        String huge = "FormatException: " + "a".repeat(600);
        ClientEventParser.Parsed fatal = ClientEventParser.parse(
                mapper.readTree(base("message", huge).replace("\"stackTrace\": \"#0 Foo\"",
                        "\"stackTrace\": \"" + "s".repeat(9000) + "\"")),
                limits, clock);
        assertEquals(500, fatal.message().length());
        assertEquals(8192, fatal.stackTrace().length());

        ClientEventParser.Parsed api = ClientEventParser.parse(mapper.readTree("""
                {
                  "eventId": "11111111-1111-4111-8111-111111111111",
                  "type": "apiError",
                  "timestamp": "2026-11-02T06:15:00.000Z",
                  "message": "badResponse 401 GET /cars/vin/*",
                  "stackTrace": "should be ignored",
                  "sessionId": "22222222-2222-4222-8222-222222222222",
                  "installId": "33333333-3333-4333-8333-333333333333",
                  "app": {"version": "1.4.0", "buildNumber": "42", "packageName": "com.nematmirzayev.carcat"},
                  "os": {"name": "ios", "version": "18.0"},
                  "environment": "release",
                  "context": {"statusCode": 401, "endpoint": "GET /cars/vin/*"}
                }
                """), limits, clock);
        assertNull(api.stackTrace());
        assertEquals(401, api.statusCode());
        assertEquals("GET /cars/vin/*", api.endpoint());
    }

    @Test
    void timeoutUsesTheMessageWhenContextHasNoStatus() throws Exception {
        ClientEventParser.Parsed api = ClientEventParser.parse(mapper.readTree("""
                {
                  "eventId": "11111111-1111-4111-8111-111111111111",
                  "type": "apiError",
                  "timestamp": "2026-11-02T06:15:00.000Z",
                  "message": "connectionTimeout GET /cars/vin/*",
                  "sessionId": "22222222-2222-4222-8222-222222222222",
                  "installId": "33333333-3333-4333-8333-333333333333",
                  "app": {"version": "1.4.0", "buildNumber": "42", "packageName": "com.nematmirzayev.carcat"},
                  "os": {"name": "ios", "version": "18.0"},
                  "environment": "profile",
                  "context": {"endpoint": "GET /cars/vin/*"}
                }
                """), limits, clock);
        assertNull(api.statusCode());
        assertEquals("connectionTimeout", api.signal());
        assertEquals("GET /cars/vin/*", api.endpoint());
    }

    @Test
    void keepsThirtyBreadcrumbs() throws Exception {
        StringBuilder crumbs = new StringBuilder("[");
        for (int i = 0; i < 35; i++) {
            if (i > 0) {
                crumbs.append(',');
            }
            crumbs.append("{\"timestamp\":\"2026-11-02T06:15:00.000Z\",\"category\":\"navigation\",\"message\":\"p")
                    .append(i).append("\"}");
        }
        crumbs.append(']');
        String json = """
                {
                  "eventId": "11111111-1111-4111-8111-111111111111",
                  "type": "fatal",
                  "timestamp": "2026-11-02T06:15:00.000Z",
                  "message": "FormatException: boom",
                  "sessionId": "22222222-2222-4222-8222-222222222222",
                  "installId": "33333333-3333-4333-8333-333333333333",
                  "userId": 15,
                  "app": {"version": "1.4.0", "buildNumber": "42", "packageName": "com.nematmirzayev.carcat"},
                  "os": {"name": "ios", "version": "18.0"},
                  "environment": "release",
                  "breadcrumbs": %s
                }
                """.formatted(crumbs);
        ClientEventParser.Parsed parsed = ClientEventParser.parse(mapper.readTree(json), limits, clock);
        assertEquals(30, parsed.breadcrumbs().size());
    }

    private static String base(String field, String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return """
                {
                  "eventId": "11111111-1111-4111-8111-111111111111",
                  "type": "fatal",
                  "timestamp": "2026-11-02T06:15:00.000Z",
                  "message": "FormatException: boom",
                  "stackTrace": "#0 Foo",
                  "sessionId": "22222222-2222-4222-8222-222222222222",
                  "installId": "33333333-3333-4333-8333-333333333333",
                  "userId": 15,
                  "app": {"version": "1.4.0", "buildNumber": "42", "packageName": "com.nematmirzayev.carcat"},
                  "os": {"name": "ios", "version": "18.0"},
                  "environment": "release"
                }
                """.replace("\"" + field + "\": \"FormatException: boom\"", "\"" + field + "\": \"" + escaped + "\"")
                .replace("\"" + field + "\": \"33333333-3333-4333-8333-333333333333\"", "\"" + field + "\": \"" + escaped + "\"")
                .replace("\"" + field + "\": \"release\"", "\"" + field + "\": \"" + escaped + "\"");
    }
}
