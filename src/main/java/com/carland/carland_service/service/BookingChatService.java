package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.ChatConversationView;
import com.carland.carland_service.dto.booking.ChatMessageView;
import com.carland.carland_service.dto.booking.ChatMessagesPage;
import com.carland.carland_service.dto.booking.ChatSendRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingMessage;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingMessageRepository;
import com.carland.carland_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * tr: Rezervasyon sohbeti — metin listele/gönder + SSE (CRCT-286 faz 1).
 * en: Booking chat — list/send text + SSE (CRCT-286 phase 1).
 */
@Service
@RequiredArgsConstructor
public class BookingChatService {

    private static final int DEFAULT_LIMIT = 40;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_TEXT = 2000;
    private static final DateTimeFormatter UTC_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(java.time.ZoneOffset.UTC);

    private final BookingRepository bookingRepository;
    private final BookingMessageRepository messageRepository;
    private final BookingStaffAccess bookingStaffAccess;
    private final BookingChatLiveService chatLiveService;

    @Transactional(readOnly = true)
    public ChatMessagesPage ownerMessages(Long customerUserId, String bookingKey, String cursor, Integer limit) {
        Booking booking = loadOwned(customerUserId, bookingKey);
        return pageOf(booking, cursor, limit);
    }

    @Transactional
    public ChatMessageView ownerSend(Long customerUserId, String bookingKey, ChatSendRequest request) {
        Booking booking = loadOwned(customerUserId, bookingKey);
        return send(booking, "customer", customerUserId, request);
    }

    @Transactional(readOnly = true)
    public SseEmitter ownerLive(Long customerUserId, String bookingKey) {
        Booking booking = loadOwned(customerUserId, bookingKey);
        return chatLiveService.listen(booking.getId());
    }

    @Transactional(readOnly = true)
    public ChatMessagesPage staffMessages(
            Long userId, boolean mustChangePassword, Long bookingId, String cursor, Integer limit, String acceptLanguage) {
        Booking booking = loadStaffBooking(userId, mustChangePassword, bookingId, acceptLanguage);
        return pageOf(booking, cursor, limit);
    }

    @Transactional
    public ChatMessageView staffSend(
            Long userId, boolean mustChangePassword, Long bookingId, ChatSendRequest request, String acceptLanguage) {
        Booking booking = loadStaffBooking(userId, mustChangePassword, bookingId, acceptLanguage);
        return send(booking, "branch", userId, request);
    }

    @Transactional(readOnly = true)
    public SseEmitter staffLive(Long userId, boolean mustChangePassword, Long bookingId, String acceptLanguage) {
        Booking booking = loadStaffBooking(userId, mustChangePassword, bookingId, acceptLanguage);
        return chatLiveService.listen(booking.getId());
    }

    private ChatMessageView send(Booking booking, String fromRole, Long authorUserId, ChatSendRequest request) {
        requireOpen(booking);
        if (request == null) {
            throw MissingFieldException.required("body");
        }
        String clientMessageId = trimToNull(request.getClientMessageId());
        if (clientMessageId == null) {
            throw MissingFieldException.required("clientMessageId");
        }
        if (clientMessageId.length() > 64) {
            throw MissingFieldException.required("clientMessageId");
        }
        String kind = request.getKind() == null ? "text" : request.getKind().trim().toLowerCase(Locale.ROOT);
        if (!"text".equals(kind)) {
            throw new MissingFieldException("only text messages in this phase");
        }
        String text = request.getText() == null ? "" : request.getText().trim();
        if (text.isEmpty()) {
            throw MissingFieldException.required("text");
        }
        if (text.length() > MAX_TEXT) {
            throw new MissingFieldException("Text exceeds 2000 characters");
        }

        var existing = messageRepository.findByBooking_IdAndClientMessageId(booking.getId(), clientMessageId);
        if (existing.isPresent()) {
            return toView(existing.get());
        }

        BookingMessage row = BookingMessage.builder()
                .booking(booking)
                .clientMessageId(clientMessageId)
                .fromRole(fromRole)
                .authorUserId(authorUserId)
                .kind("text")
                .body(text)
                .delivery("sent")
                .rebook(false)
                .createdAt(OffsetDateTime.now())
                .build();
        row = messageRepository.save(row);
        ChatMessageView view = toView(row);
        chatLiveService.publishNewMessageAfterCommit(booking.getId(), view);
        return view;
    }

    private ChatMessagesPage pageOf(Booking booking, String cursor, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<BookingMessage> newestFirst;
        if (cursor == null || cursor.isBlank()) {
            newestFirst = messageRepository.findLatest(booking.getId(), PageRequest.of(0, size + 1));
        } else {
            CursorDecoded decoded = decodeCursor(cursor);
            newestFirst = messageRepository.findOlderThan(
                    booking.getId(), decoded.createdAt(), decoded.id(), PageRequest.of(0, size + 1));
        }
        boolean hasMore = newestFirst.size() > size;
        if (hasMore) {
            newestFirst = new ArrayList<>(newestFirst.subList(0, size));
        }
        String nextCursor = null;
        if (hasMore && !newestFirst.isEmpty()) {
            BookingMessage oldestInPage = newestFirst.get(newestFirst.size() - 1);
            nextCursor = encodeCursor(oldestInPage.getCreatedAt(), oldestInPage.getId());
        }
        List<BookingMessage> ascending = new ArrayList<>(newestFirst);
        Collections.reverse(ascending);
        List<ChatMessageView> messages = ascending.stream().map(this::toView).toList();
        return ChatMessagesPage.builder()
                .messages(messages)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .conversation(conversationOf(booking))
                .build();
    }

    private ChatConversationView conversationOf(Booking booking) {
        return ChatConversationView.builder()
                .reservationId(String.valueOf(booking.getId()))
                .state(stateOf(booking))
                .providerTyping(false)
                .build();
    }

    /** MVP: only pending is writable (approval wait). */
    static String stateOf(Booking booking) {
        String status = booking.getStatus() == null ? "" : booking.getStatus().trim().toLowerCase(Locale.ROOT);
        if (BookingStatus.PENDING.apiValue().equals(status)) {
            return "open";
        }
        return "closed";
    }

    private void requireOpen(Booking booking) {
        if (!"open".equals(stateOf(booking))) {
            throw new ConflictException("conversationClosed");
        }
    }

    private Booking loadStaffBooking(Long userId, boolean mustChangePassword, Long bookingId, String acceptLanguage) {
        if (bookingId == null) {
            throw MissingFieldException.required("bookingId");
        }
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        Long branchId = booking.getBranch() == null ? null : booking.getBranch().getId();
        bookingStaffAccess.requireWritableBranch(staff, branchId, acceptLanguage);
        return booking;
    }

    private Booking loadOwned(Long customerUserId, String bookingKey) {
        if (bookingKey == null || bookingKey.isBlank()) {
            throw MissingFieldException.required("bookingId");
        }
        Booking booking;
        if (bookingKey.regionMatches(true, 0, "CC-", 0, 3)) {
            booking = bookingRepository.findByRef(bookingKey.toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        } else {
            Long id;
            try {
                id = Long.valueOf(bookingKey.trim());
            } catch (NumberFormatException ex) {
                throw new ResourceNotFoundException("booking not found");
            }
            booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        }
        if (booking.getCustomerUserId() == null || !booking.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("booking is not yours");
        }
        return booking;
    }

    private ChatMessageView toView(BookingMessage row) {
        String authorId = row.getAuthorUserId() == null
                ? ("system".equals(row.getFromRole()) ? "system" : null)
                : String.valueOf(row.getAuthorUserId());
        return ChatMessageView.builder()
                .clientMessageId(row.getClientMessageId())
                .serverId(row.getId() == null ? null : String.valueOf(row.getId()))
                .authorId(authorId)
                .fromRole(row.getFromRole())
                .createdAt(stampUtc(row.getCreatedAt()))
                .kind(row.getKind())
                .text(row.getBody())
                .delivery(row.getDelivery())
                .rebook(Boolean.TRUE.equals(row.getRebook()))
                .attachment(null)
                .build();
    }

    static String stampUtc(OffsetDateTime time) {
        if (time == null) {
            return null;
        }
        return UTC_MILLIS.format(time.toInstant());
    }

    private static String encodeCursor(OffsetDateTime createdAt, Long id) {
        String raw = stampUtc(createdAt) + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static CursorDecoded decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int cut = raw.lastIndexOf('|');
            if (cut <= 0) {
                throw new IllegalArgumentException("bad cursor");
            }
            OffsetDateTime createdAt = OffsetDateTime.parse(raw.substring(0, cut));
            Long id = Long.valueOf(raw.substring(cut + 1));
            return new CursorDecoded(createdAt, id);
        } catch (Exception ex) {
            throw new ConflictException("invalid cursor");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record CursorDecoded(OffsetDateTime createdAt, Long id) {
    }
}
