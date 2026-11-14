package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.ChatAttachmentView;
import com.carland.carland_service.dto.booking.ChatConversationView;
import com.carland.carland_service.dto.booking.ChatMessageView;
import com.carland.carland_service.dto.booking.ChatMessagesPage;
import com.carland.carland_service.dto.booking.ChatReadRequest;
import com.carland.carland_service.dto.booking.ChatSendRequest;
import com.carland.carland_service.dto.booking.ChatTypingRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingChatUpload;
import com.carland.carland_service.entity.BookingMessage;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingChatUploadRepository;
import com.carland.carland_service.repository.BookingMessageRepository;
import com.carland.carland_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * tr: Rezervasyon sohbeti — metin/medya + SSE + iletildi/görüldü/yazıyor (CRCT-286).
 * en: Booking chat — text/media + SSE + delivered/seen/typing (CRCT-286).
 */
@Service
@RequiredArgsConstructor
public class BookingChatService {

    private static final int DEFAULT_LIMIT = 40;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_TEXT = 2000;
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final Set<String> FILE_EXT = Set.of("pdf", "doc", "docx", "xls", "xlsx", "txt");
    private static final DateTimeFormatter UTC_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(java.time.ZoneOffset.UTC);

    private final BookingRepository bookingRepository;
    private final BookingMessageRepository messageRepository;
    private final BookingChatUploadRepository uploadRepository;
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

    @Transactional
    public SseEmitter ownerLive(Long customerUserId, String bookingKey) {
        Booking booking = loadOwned(customerUserId, bookingKey);
        SseEmitter emitter = chatLiveService.listen(booking.getId(), "customer");
        markPeerDelivered(booking, "customer");
        return emitter;
    }

    @Transactional
    public void ownerRead(Long customerUserId, String bookingKey, ChatReadRequest request) {
        Booking booking = loadOwned(customerUserId, bookingKey);
        markSeen(booking, "customer", request);
    }

    @Transactional(readOnly = true)
    public void ownerTyping(Long customerUserId, String bookingKey, ChatTypingRequest request) {
        Booking booking = loadOwned(customerUserId, bookingKey);
        requireOpen(booking);
        boolean typing = request != null && Boolean.TRUE.equals(request.getIsTyping());
        chatLiveService.publishTyping(booking.getId(), String.valueOf(customerUserId), "customer", typing);
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

    @Transactional
    public SseEmitter staffLive(Long userId, boolean mustChangePassword, Long bookingId, String acceptLanguage) {
        Booking booking = loadStaffBooking(userId, mustChangePassword, bookingId, acceptLanguage);
        SseEmitter emitter = chatLiveService.listen(booking.getId(), "branch");
        markPeerDelivered(booking, "branch");
        return emitter;
    }

    @Transactional
    public void staffRead(
            Long userId, boolean mustChangePassword, Long bookingId, ChatReadRequest request, String acceptLanguage) {
        Booking booking = loadStaffBooking(userId, mustChangePassword, bookingId, acceptLanguage);
        markSeen(booking, "branch", request);
    }

    @Transactional(readOnly = true)
    public void staffTyping(
            Long userId, boolean mustChangePassword, Long bookingId, ChatTypingRequest request, String acceptLanguage) {
        Booking booking = loadStaffBooking(userId, mustChangePassword, bookingId, acceptLanguage);
        requireOpen(booking);
        boolean typing = request != null && Boolean.TRUE.equals(request.getIsTyping());
        chatLiveService.publishTyping(booking.getId(), String.valueOf(userId), "branch", typing);
    }

    @Transactional
    public ChatAttachmentView ownerUpload(
            Long customerUserId, String bookingKey, MultipartFile file, String kind, String clientMessageId,
            String fileName, Integer width, Integer height) {
        Booking booking = loadOwned(customerUserId, bookingKey);
        return storeUpload(booking, "customer", customerUserId, file, kind, clientMessageId, fileName, width, height);
    }

    @Transactional
    public ChatAttachmentView staffUpload(
            Long userId, boolean mustChangePassword, Long bookingId, MultipartFile file, String kind,
            String clientMessageId, String fileName, Integer width, Integer height, String acceptLanguage) {
        Booking booking = loadStaffBooking(userId, mustChangePassword, bookingId, acceptLanguage);
        return storeUpload(booking, "branch", userId, file, kind, clientMessageId, fileName, width, height);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> ownerDownload(Long customerUserId, String bookingKey, String uploadId) {
        Booking booking = loadOwned(customerUserId, bookingKey);
        return download(booking, uploadId);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> staffDownload(
            Long userId, boolean mustChangePassword, Long bookingId, String uploadId, String acceptLanguage) {
        Booking booking = loadStaffBooking(userId, mustChangePassword, bookingId, acceptLanguage);
        return download(booking, uploadId);
    }

    private ChatAttachmentView storeUpload(
            Booking booking, String fromRole, Long userId, MultipartFile file, String kindRaw,
            String clientMessageId, String fileNameHint, Integer width, Integer height) {
        requireOpen(booking);
        if (file == null || file.isEmpty()) {
            throw MissingFieldException.required("file");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new MissingFieldException("payloadTooLarge");
        }
        String kind = kindRaw == null ? "" : kindRaw.trim().toLowerCase(Locale.ROOT);
        if (!"image".equals(kind) && !"file".equals(kind)) {
            throw MissingFieldException.required("kind");
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType().toLowerCase(Locale.ROOT);
        String fileName = trimToNull(fileNameHint);
        if (fileName == null) {
            fileName = trimToNull(file.getOriginalFilename());
        }
        if (fileName == null) {
            fileName = "file";
        }
        if ("image".equals(kind)) {
            if (!IMAGE_TYPES.contains(contentType) && !looksLikeImageName(fileName)) {
                throw new MissingFieldException("unsupportedMedia");
            }
            if (!IMAGE_TYPES.contains(contentType)) {
                contentType = "image/jpeg";
            }
        } else {
            String ext = extensionOf(fileName);
            if (!FILE_EXT.contains(ext)) {
                throw new MissingFieldException("unsupportedMedia");
            }
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new MissingFieldException("Failed to read file");
        }
        String publicId = "upl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        BookingChatUpload upload = BookingChatUpload.builder()
                .publicId(publicId)
                .booking(booking)
                .kind(kind)
                .fileName(fileName)
                .contentType(contentType)
                .sizeBytes((long) bytes.length)
                .width("image".equals(kind) ? width : null)
                .height("image".equals(kind) ? height : null)
                .data(bytes)
                .createdByRole(fromRole)
                .createdByUserId(userId)
                .createdAt(OffsetDateTime.now())
                .build();
        uploadRepository.save(upload);
        return attachmentOf(booking.getId(), upload, false);
    }

    private ResponseEntity<byte[]> download(Booking booking, String uploadId) {
        String id = trimToNull(uploadId);
        if (id == null) {
            throw MissingFieldException.required("uploadId");
        }
        BookingChatUpload upload = uploadRepository.findByBooking_IdAndPublicId(booking.getId(), id)
                .orElseThrow(() -> new ResourceNotFoundException("file not found"));
        if (upload.getData() == null || upload.getData().length == 0) {
            throw new ResourceNotFoundException("file unavailable");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitizeFileName(upload.getFileName()) + "\"")
                .contentType(MediaType.parseMediaType(upload.getContentType()))
                .contentLength(upload.getSizeBytes() == null ? upload.getData().length : upload.getSizeBytes())
                .body(upload.getData());
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
        if (!Set.of("text", "image", "file").contains(kind)) {
            throw new MissingFieldException("unsupported kind");
        }

        var existing = messageRepository.findByBooking_IdAndClientMessageId(booking.getId(), clientMessageId);
        if (existing.isPresent()) {
            return toView(existing.get());
        }

        String text = request.getText() == null ? "" : request.getText().trim();
        BookingChatUpload upload = null;
        if ("text".equals(kind)) {
            if (text.isEmpty()) {
                throw MissingFieldException.required("text");
            }
            if (text.length() > MAX_TEXT) {
                throw new MissingFieldException("Text exceeds 2000 characters");
            }
        } else {
            String uploadId = trimToNull(request.getUploadId());
            if (uploadId == null) {
                throw MissingFieldException.required("uploadId");
            }
            upload = uploadRepository.findByBooking_IdAndPublicId(booking.getId(), uploadId)
                    .orElseThrow(() -> new ResourceNotFoundException("upload not found"));
            if (upload.getMessageId() != null) {
                throw new ConflictException("upload already used");
            }
            if (!kind.equals(upload.getKind())) {
                throw new MissingFieldException("kind does not match upload");
            }
            if (text.length() > MAX_TEXT) {
                throw new MissingFieldException("Text exceeds 2000 characters");
            }
        }

        boolean peerOnline = chatLiveService.hasPeerListener(booking.getId(), fromRole);
        String delivery = peerOnline ? "delivered" : "sent";

        BookingMessage.BookingMessageBuilder builder = BookingMessage.builder()
                .booking(booking)
                .clientMessageId(clientMessageId)
                .fromRole(fromRole)
                .authorUserId(authorUserId)
                .kind(kind)
                .body(text)
                .delivery(delivery)
                .rebook(false)
                .createdAt(OffsetDateTime.now());
        if (upload != null) {
            builder.uploadId(upload.getPublicId())
                    .fileName(upload.getFileName())
                    .contentType(upload.getContentType())
                    .sizeBytes(upload.getSizeBytes())
                    .width(upload.getWidth())
                    .height(upload.getHeight());
            if (request.getWidth() != null) {
                builder.width(request.getWidth());
            }
            if (request.getHeight() != null) {
                builder.height(request.getHeight());
            }
        }
        BookingMessage row = messageRepository.save(builder.build());
        if (upload != null) {
            upload.setMessageId(row.getId());
            if (row.getWidth() != null) {
                upload.setWidth(row.getWidth());
            }
            if (row.getHeight() != null) {
                upload.setHeight(row.getHeight());
            }
            uploadRepository.save(upload);
        }
        ChatMessageView view = toView(row);
        chatLiveService.publishNewMessageAfterCommit(booking.getId(), view);
        if (peerOnline) {
            chatLiveService.publishDelivery(
                    booking.getId(), row.getClientMessageId(), String.valueOf(row.getId()), "delivered");
        }
        return view;
    }

    private void markPeerDelivered(Booking booking, String viewerRole) {
        List<BookingMessage> pending = messageRepository.findByBooking_IdAndFromRoleNotAndDelivery(
                booking.getId(), viewerRole, "sent");
        if (pending.isEmpty()) {
            return;
        }
        for (BookingMessage row : pending) {
            row.setDelivery("delivered");
        }
        messageRepository.saveAll(pending);
        for (BookingMessage row : pending) {
            chatLiveService.publishDelivery(
                    booking.getId(),
                    row.getClientMessageId(),
                    row.getId() == null ? null : String.valueOf(row.getId()),
                    "delivered");
        }
    }

    private void markSeen(Booking booking, String viewerRole, ChatReadRequest request) {
        if (request == null || request.getClientMessageIds() == null || request.getClientMessageIds().isEmpty()) {
            throw MissingFieldException.required("clientMessageIds");
        }
        List<String> ids = request.getClientMessageIds().stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            throw MissingFieldException.required("clientMessageIds");
        }
        List<BookingMessage> rows = messageRepository.findByBooking_IdAndClientMessageIdIn(booking.getId(), ids);
        List<String> marked = new ArrayList<>();
        for (BookingMessage row : rows) {
            if (viewerRole.equals(row.getFromRole())) {
                continue; // kendi mesajını seen yapma
            }
            if ("seen".equals(row.getDelivery())) {
                marked.add(row.getClientMessageId());
                continue;
            }
            row.setDelivery("seen");
            marked.add(row.getClientMessageId());
        }
        if (!marked.isEmpty()) {
            messageRepository.saveAll(rows);
            chatLiveService.publishRead(booking.getId(), marked);
        }
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
        ChatAttachmentView attachment = null;
        if (row.getUploadId() != null && ("image".equals(row.getKind()) || "file".equals(row.getKind()))) {
            attachment = ChatAttachmentView.builder()
                    .uploadId(row.getUploadId())
                    .kind(row.getKind())
                    .url(fileUrl(row.getBooking().getId(), row.getUploadId()))
                    .fileName(row.getFileName())
                    .sizeBytes(row.getSizeBytes())
                    .width(row.getWidth())
                    .height(row.getHeight())
                    .unavailable(false)
                    .build();
        }
        return ChatMessageView.builder()
                .clientMessageId(row.getClientMessageId())
                .serverId(row.getId() == null ? null : String.valueOf(row.getId()))
                .authorId(authorId)
                .fromRole(row.getFromRole())
                .createdAt(stampUtc(row.getCreatedAt()))
                .kind(row.getKind())
                .text(row.getBody() == null || row.getBody().isBlank() ? null : row.getBody())
                .delivery(row.getDelivery())
                .rebook(Boolean.TRUE.equals(row.getRebook()))
                .attachment(attachment)
                .build();
    }

    private ChatAttachmentView attachmentOf(Long bookingId, BookingChatUpload upload, boolean unavailable) {
        return ChatAttachmentView.builder()
                .uploadId(upload.getPublicId())
                .kind(upload.getKind())
                .url(fileUrl(bookingId, upload.getPublicId()))
                .fileName(upload.getFileName())
                .sizeBytes(upload.getSizeBytes())
                .width(upload.getWidth())
                .height(upload.getHeight())
                .unavailable(unavailable)
                .build();
    }

    private static String fileUrl(Long bookingId, String uploadId) {
        return "/api/v1/booking/bookings/" + bookingId + "/uploads/" + uploadId + "/file";
    }

    private static boolean looksLikeImageName(String fileName) {
        String ext = extensionOf(fileName);
        return Set.of("jpg", "jpeg", "png", "webp").contains(ext);
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitizeFileName(String name) {
        if (name == null) {
            return "file";
        }
        return name.replace("\"", "").replace("\r", "").replace("\n", "");
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
