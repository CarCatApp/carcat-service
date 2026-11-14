package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.ChatAttachmentView;
import com.carland.carland_service.dto.booking.ChatMessageView;
import com.carland.carland_service.dto.booking.ChatMessagesPage;
import com.carland.carland_service.dto.booking.ChatReadRequest;
import com.carland.carland_service.dto.booking.ChatSendRequest;
import com.carland.carland_service.dto.booking.ChatTypingRequest;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.service.BookingChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * tr: Owner + staff rezervasyon sohbeti (CRCT-286).
 * en: Owner + staff booking chat (CRCT-286).
 */
@RestController
@RequiredArgsConstructor
public class BookingChatController {

    private final BookingChatService bookingChatService;
    private final BookingStaffRequestAuth bookingStaffRequestAuth;

    @GetMapping("/api/v1/booking/bookings/{bookingId}/messages")
    public ChatMessagesPage ownerMessages(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String bookingId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return bookingChatService.ownerMessages(parseUserId(userIdHeader), bookingId, cursor, limit);
    }

    @PostMapping("/api/v1/booking/bookings/{bookingId}/messages")
    public ResponseEntity<ChatMessageView> ownerSend(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String bookingId,
            @RequestBody(required = false) ChatSendRequest request
    ) {
        ChatMessageView view = bookingChatService.ownerSend(parseUserId(userIdHeader), bookingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PostMapping("/api/v1/booking/bookings/{bookingId}/read")
    public ResponseEntity<Void> ownerRead(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String bookingId,
            @RequestBody(required = false) ChatReadRequest request
    ) {
        bookingChatService.ownerRead(parseUserId(userIdHeader), bookingId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/booking/bookings/{bookingId}/typing")
    public ResponseEntity<Void> ownerTyping(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String bookingId,
            @RequestBody(required = false) ChatTypingRequest request
    ) {
        bookingChatService.ownerTyping(parseUserId(userIdHeader), bookingId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/api/v1/booking/bookings/{bookingId}/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatAttachmentView> ownerUpload(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String bookingId,
            @RequestPart("file") MultipartFile file,
            @RequestParam String kind,
            @RequestParam(required = false) String clientMessageId,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                bookingChatService.ownerUpload(
                        parseUserId(userIdHeader), bookingId, file, kind, clientMessageId, fileName, width, height));
    }

    @GetMapping("/api/v1/booking/bookings/{bookingId}/uploads/{uploadId}/file")
    public ResponseEntity<byte[]> ownerDownload(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String bookingId,
            @PathVariable String uploadId
    ) {
        return bookingChatService.ownerDownload(parseUserId(userIdHeader), bookingId, uploadId);
    }

    @GetMapping(value = "/api/v1/booking/bookings/{bookingId}/messages/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ownerLive(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String bookingId,
            HttpServletResponse response
    ) {
        prepareSse(response);
        return bookingChatService.ownerLive(parseUserId(userIdHeader), bookingId);
    }

    @GetMapping("/api/v1/booking/staff/bookings/{bookingId}/messages")
    public ChatMessagesPage staffMessages(
            HttpServletRequest request,
            @PathVariable Long bookingId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        return bookingChatService.staffMessages(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                bookingId, cursor, limit, acceptLanguage);
    }

    @PostMapping("/api/v1/booking/staff/bookings/{bookingId}/messages")
    public ResponseEntity<ChatMessageView> staffSend(
            HttpServletRequest request,
            @PathVariable Long bookingId,
            @RequestBody(required = false) ChatSendRequest requestBody,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        ChatMessageView view = bookingChatService.staffSend(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                bookingId, requestBody, acceptLanguage);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PostMapping("/api/v1/booking/staff/bookings/{bookingId}/read")
    public ResponseEntity<Void> staffRead(
            HttpServletRequest request,
            @PathVariable Long bookingId,
            @RequestBody(required = false) ChatReadRequest requestBody,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        bookingChatService.staffRead(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                bookingId, requestBody, acceptLanguage);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/booking/staff/bookings/{bookingId}/typing")
    public ResponseEntity<Void> staffTyping(
            HttpServletRequest request,
            @PathVariable Long bookingId,
            @RequestBody(required = false) ChatTypingRequest requestBody,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        bookingChatService.staffTyping(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                bookingId, requestBody, acceptLanguage);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/api/v1/booking/staff/bookings/{bookingId}/uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatAttachmentView> staffUpload(
            HttpServletRequest request,
            @PathVariable Long bookingId,
            @RequestPart("file") MultipartFile file,
            @RequestParam String kind,
            @RequestParam(required = false) String clientMessageId,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                bookingChatService.staffUpload(
                        bookingStaffRequestAuth.userId(request),
                        bookingStaffRequestAuth.mustChangePassword(request),
                        bookingId, file, kind, clientMessageId, fileName, width, height, acceptLanguage));
    }

    @GetMapping("/api/v1/booking/staff/bookings/{bookingId}/uploads/{uploadId}/file")
    public ResponseEntity<byte[]> staffDownload(
            HttpServletRequest request,
            @PathVariable Long bookingId,
            @PathVariable String uploadId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        return bookingChatService.staffDownload(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                bookingId, uploadId, acceptLanguage);
    }

    @GetMapping(
            value = "/api/v1/booking/staff/bookings/{bookingId}/messages/live",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter staffLive(
            HttpServletRequest request,
            @PathVariable Long bookingId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        prepareSse(response);
        return bookingChatService.staffLive(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                bookingId, acceptLanguage);
    }

    private static void prepareSse(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Content-Encoding", "identity");
    }

    private static Long parseUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw MissingFieldException.required("X-User-Id");
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw MissingFieldException.required("X-User-Id");
        }
    }
}
