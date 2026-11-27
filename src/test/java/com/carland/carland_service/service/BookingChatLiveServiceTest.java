package com.carland.carland_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingChatLiveServiceTest {

    @Test
    void connectSendsBothRolesAndDropMarksPeerOffline() throws Exception {
        BookingChatLiveService live = new BookingChatLiveService(new ObjectMapper());
        SseEmitter customer = live.listen(9L, "customer");

        String alone = text(customer);
        assertTrue(lastPresence(alone, "customer").contains("\"online\":true"));
        assertTrue(lastPresence(alone, "branch").contains("\"online\":false"));

        SseEmitter branch = live.listen(9L, "branch");
        assertTrue(lastPresence(text(customer), "branch").contains("\"online\":true"));
        assertTrue(lastPresence(text(branch), "customer").contains("\"online\":true"));

        disconnect(branch);
        assertTrue(lastPresence(text(customer), "branch").contains("\"online\":false"));
        assertFalse(live.hasPeerListener(9L, "customer"));
    }

    @Test
    void branchStaysOnlineUntilLastListenerDrops() throws Exception {
        BookingChatLiveService live = new BookingChatLiveService(new ObjectMapper());
        SseEmitter customer = live.listen(9L, "customer");
        SseEmitter branchA = live.listen(9L, "branch");
        SseEmitter branchB = live.listen(9L, "branch");

        disconnect(branchA);
        assertTrue(lastPresence(text(customer), "branch").contains("\"online\":true"));
        assertTrue(live.hasPeerListener(9L, "customer"));

        disconnect(branchB);
        assertTrue(lastPresence(text(customer), "branch").contains("\"online\":false"));
        assertFalse(live.hasPeerListener(9L, "customer"));
    }

    @Test
    void customerDropReachesBranch() throws Exception {
        BookingChatLiveService live = new BookingChatLiveService(new ObjectMapper());
        SseEmitter customer = live.listen(4L, "customer");
        SseEmitter branch = live.listen(4L, "branch");

        disconnect(customer);
        assertTrue(lastPresence(text(branch), "customer").contains("\"online\":false"));
        assertFalse(live.hasPeerListener(4L, "branch"));
    }

    private static String lastPresence(String raw, String role) {
        String needle = "\"role\":\"" + role + "\"";
        int at = raw.lastIndexOf(needle);
        assertTrue(at >= 0, raw);
        int start = raw.lastIndexOf('{', at);
        int end = raw.indexOf('}', at);
        return raw.substring(start, end + 1);
    }

    @SuppressWarnings("unchecked")
    private static String text(SseEmitter emitter) throws Exception {
        Field field = ResponseBodyEmitter.class.getDeclaredField("earlySendAttempts");
        field.setAccessible(true);
        Set<ResponseBodyEmitter.DataWithMediaType> items =
                (Set<ResponseBodyEmitter.DataWithMediaType>) field.get(emitter);
        StringBuilder raw = new StringBuilder();
        for (ResponseBodyEmitter.DataWithMediaType item : items) {
            raw.append(item.getData());
        }
        return raw.toString();
    }

    private static void disconnect(SseEmitter emitter) throws Exception {
        Field field = ResponseBodyEmitter.class.getDeclaredField("completionCallback");
        field.setAccessible(true);
        ((Runnable) field.get(emitter)).run();
    }
}
