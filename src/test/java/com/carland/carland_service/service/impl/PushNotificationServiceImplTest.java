package com.carland.carland_service.service.impl;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PushNotificationServiceImplTest {

    @Test
    void iosMessagesCarryTheCustomSound() {
        Message single = PushNotificationServiceImpl.buildMessage("Başlıq", "Mətn", "token-1");
        MulticastMessage batch = PushNotificationServiceImpl.buildMulticast("Başlıq", "Mətn", List.of("token-1", "token-2"));

        assertTrue(contains(single, "Başlıq"), "title");
        assertTrue(contains(single, PushNotificationServiceImpl.IOS_SOUND), describe(single));
        assertTrue(contains(batch, PushNotificationServiceImpl.IOS_SOUND), describe(batch));
    }

    private static boolean contains(Object value, String needle) {
        return walk(value, needle, 0, new IdentityHashMap<>());
    }

    private static String describe(Object value) {
        return value.getClass().getName();
    }

    private static boolean walk(Object value, String needle, int depth, IdentityHashMap<Object, Boolean> seen) {
        if (value == null || depth > 8 || seen.containsKey(value)) {
            return false;
        }
        if (value instanceof String text) {
            return text.contains(needle);
        }
        if (value instanceof Map<?, ?> map) {
            seen.put(value, Boolean.TRUE);
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (walk(entry.getKey(), needle, depth + 1, seen) || walk(entry.getValue(), needle, depth + 1, seen)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Iterable<?> iterable) {
            seen.put(value, Boolean.TRUE);
            for (Object item : iterable) {
                if (walk(item, needle, depth + 1, seen)) {
                    return true;
                }
            }
            return false;
        }
        Class<?> type = value.getClass();
        if (type.isPrimitive() || type.getName().startsWith("java.")) {
            return String.valueOf(value).contains(needle);
        }
        seen.put(value, Boolean.TRUE);
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    if (walk(field.get(value), needle, depth + 1, seen)) {
                        return true;
                    }
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
