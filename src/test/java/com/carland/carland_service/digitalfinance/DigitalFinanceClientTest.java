package com.carland.carland_service.digitalfinance;

import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DigitalFinanceClientTest {

    @Test
    void writesUpstreamJsonBytesUnchanged() throws Exception {
        byte[] upstream = ("{\"RequestIdentifier\":\"abc\",\"Status\":{\"Name\":\"Successful\",\"Code\":0,\"Message\":\"\"},"
                + "\"Response\":{\"Balance\":0.0}}").getBytes(StandardCharsets.UTF_8);
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> requestId = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/Info/Balance", exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("ApiKey"));
            requestId.set(exchange.getRequestHeaders().getFirst("RequestIdentifier"));
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, upstream.length);
            exchange.getResponseBody().write(upstream);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            DigitalFinanceClient client = new DigitalFinanceClient(
                    "http://127.0.0.1:" + port, "test-key", java.net.http.HttpClient.newHttpClient());
            CapturedResponse response = new CapturedResponse();
            client.forward("GET", "/api/v1/Info/Balance", null, "abc", response.http);
            assertEquals("test-key", apiKey.get());
            assertEquals("abc", requestId.get());
            assertEquals(200, response.status);
            assertEquals("application/json; charset=utf-8", response.contentType);
            assertArrayEquals(upstream, response.body.toByteArray());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void postForwardsBodyBytes() throws Exception {
        byte[] sent = "{\"StartDate\":\"2026-10-01\",\"EndDate\":\"2026-10-21\",\"Offset\":0,\"Limit\":20}"
                .getBytes(StandardCharsets.UTF_8);
        byte[] upstream = "{\"Status\":{\"Code\":1}}".getBytes(StandardCharsets.UTF_8);
        AtomicReference<byte[]> received = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/Info/Expenses", exchange -> {
            received.set(exchange.getRequestBody().readAllBytes());
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, upstream.length);
            exchange.getResponseBody().write(upstream);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            DigitalFinanceClient client = new DigitalFinanceClient(
                    "http://127.0.0.1:" + port, "test-key", java.net.http.HttpClient.newHttpClient());
            CapturedResponse response = new CapturedResponse();
            client.forward("POST", "/api/v1/Info/Expenses", sent, null, response.http);
            assertArrayEquals(sent, received.get());
            assertArrayEquals(upstream, response.body.toByteArray());
        } finally {
            server.stop(0);
        }
    }

    private static final class CapturedResponse {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private int status;
        private String contentType;
        private final HttpServletResponse http = mock(HttpServletResponse.class);

        private CapturedResponse() throws Exception {
            ServletOutputStream stream = new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                }

                @Override
                public void write(int b) {
                    body.write(b);
                }
            };
            when(http.getOutputStream()).thenReturn(stream);
            org.mockito.Mockito.doAnswer(invocation -> {
                status = invocation.getArgument(0);
                return null;
            }).when(http).setStatus(org.mockito.ArgumentMatchers.anyInt());
            org.mockito.Mockito.doAnswer(invocation -> {
                contentType = invocation.getArgument(0);
                return null;
            }).when(http).setContentType(org.mockito.ArgumentMatchers.anyString());
        }
    }
}
