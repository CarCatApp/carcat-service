package com.carland.carland_service.service.clientevent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ClientEventFingerprintTest {

    @Test
    void numbersAndUuidsCollapse() {
        String left = ClientEventFingerprint.fatal(1, "fatal",
                "FormatException: User 123 not found", stack(10));
        String right = ClientEventFingerprint.fatal(1, "fatal",
                "FormatException: User 456 not found", stack(99));
        assertEquals(left, right);
    }

    @Test
    void uuidChangesDoNotSplitTheGroup() {
        String left = ClientEventFingerprint.fatal(1, "fatal",
                "StateError: missing 11111111-1111-4111-8111-111111111111", stack(1));
        String right = ClientEventFingerprint.fatal(1, "fatal",
                "StateError: missing 22222222-2222-4222-8222-222222222222", stack(1));
        assertEquals(left, right);
    }

    @Test
    void exceptionTypeAndEventTypeStaySeparate() {
        String format = ClientEventFingerprint.fatal(1, "fatal", "FormatException: User 1", stack(1));
        String state = ClientEventFingerprint.fatal(1, "fatal", "StateError: User 1", stack(1));
        String handled = ClientEventFingerprint.fatal(1, "nonFatal", "FormatException: User 1", stack(1));
        assertNotEquals(format, state);
        assertNotEquals(format, handled);
    }

    @Test
    void sixthFrameIsIgnored() {
        String base = frames(5) + "\n#5 Extra.frame (package:carcat/src/z.dart:1:1)";
        String otherSixth = frames(5) + "\n#5 Other.frame (package:carcat/src/z.dart:8:1)";
        assertEquals(
                ClientEventFingerprint.fatal(1, "fatal", "FormatException: boom", base),
                ClientEventFingerprint.fatal(1, "fatal", "FormatException: boom", otherSixth));
    }

    @Test
    void fifthFrameIsKept() {
        String left = frames(4) + "\n#4 Foo.a (package:carcat/src/a.dart:1:1)";
        String right = frames(4) + "\n#4 Foo.b (package:carcat/src/a.dart:1:1)";
        assertNotEquals(
                ClientEventFingerprint.fatal(1, "fatal", "FormatException: boom", left),
                ClientEventFingerprint.fatal(1, "fatal", "FormatException: boom", right));
    }

    @Test
    void apiUsesStatusAndMaskedPathAsIs() {
        String path = "GET /carland/server-carland/api/v1/cars/vin/*";
        String left = ClientEventFingerprint.api(1, 401, "badResponse", path);
        String right = ClientEventFingerprint.api(1, 401, "otherType", path);
        String otherPath = ClientEventFingerprint.api(1, 401, "badResponse", "GET /cars/vin/ABC");
        String otherStatus = ClientEventFingerprint.api(1, 500, "badResponse", path);
        assertEquals(left, right);
        assertNotEquals(left, otherPath);
        assertNotEquals(left, otherStatus);
        assertEquals(path, ClientEventFingerprint.endpointFromMessage("badResponse 401 " + path));
    }

    @Test
    void timeoutSignalSplitsGroups() {
        String path = "GET /carland/server-carland/api/v1/cars/vin/*";
        assertNotEquals(
                ClientEventFingerprint.api(1, null, "connectionTimeout", path),
                ClientEventFingerprint.api(1, null, "receiveTimeout", path));
    }

    @Test
    void versionChangesTheHash() {
        assertNotEquals(
                ClientEventFingerprint.fatal(1, "fatal", "FormatException: boom", stack(1)),
                ClientEventFingerprint.fatal(2, "fatal", "FormatException: boom", stack(1)));
    }

    private static String stack(int line) {
        return "#0 Foo.bar (package:carcat/src/a.dart:" + line + ":3)\n"
                + "#1 Baz.qux (package:carcat/src/b.dart:" + line + ":1)";
    }

    private static String frames(int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append("#").append(i).append(" Frame.m").append(i)
                    .append(" (package:carcat/src/f").append(i).append(".dart:1:1)");
        }
        return out.toString();
    }
}
