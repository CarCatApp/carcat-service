package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.ChatReadRequest;
import com.carland.carland_service.dto.booking.ChatSendRequest;
import com.carland.carland_service.dto.booking.ChatAttachmentView;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingChatUpload;
import com.carland.carland_service.entity.BookingMessage;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.repository.BookingChatUploadRepository;
import com.carland.carland_service.repository.BookingMessageRepository;
import com.carland.carland_service.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingChatServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingMessageRepository messageRepository;
    @Mock BookingChatUploadRepository uploadRepository;
    @Mock BookingStaffAccess bookingStaffAccess;
    @Mock BookingChatLiveService chatLiveService;
    @Mock BookingPushService bookingPushService;
    @InjectMocks BookingChatService service;

    @Test
    void pendingIsOpen() {
        Booking booking = Booking.builder().id(1L).status("pending").build();
        assertEquals("open", BookingChatService.stateOf(booking));
    }

    @Test
    void confirmedIsClosed() {
        Booking booking = Booking.builder().id(1L).status("confirmed").build();
        assertEquals("closed", BookingChatService.stateOf(booking));
    }

    @Test
    void ownerSendWritesAndPublishes() {
        Branch branch = Branch.builder().id(9L).build();
        Booking booking = Booking.builder()
                .id(5L)
                .status("pending")
                .customerUserId(42L)
                .branch(branch)
                .build();
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(messageRepository.findByBooking_IdAndClientMessageId(5L, "c1")).thenReturn(Optional.empty());
        when(messageRepository.save(any(BookingMessage.class))).thenAnswer(inv -> {
            BookingMessage m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        var view = service.ownerSend(42L, "5", ChatSendRequest.builder()
                .clientMessageId("c1")
                .kind("text")
                .text("salam")
                .build());

        assertEquals("c1", view.getClientMessageId());
        assertEquals("100", view.getServerId());
        assertEquals("customer", view.getFromRole());
        assertEquals("salam", view.getText());
        verify(chatLiveService).publishNewMessageAfterCommit(org.mockito.ArgumentMatchers.eq(5L), any());
        verify(bookingPushService, never()).chatNewMessage(any(), any(), any());
    }

    @Test
    void idempotentSendDoesNotInsertTwice() {
        Booking booking = Booking.builder().id(5L).status("pending").customerUserId(42L).build();
        BookingMessage existing = BookingMessage.builder()
                .id(7L)
                .booking(booking)
                .clientMessageId("c1")
                .fromRole("customer")
                .authorUserId(42L)
                .kind("text")
                .body("salam")
                .delivery("sent")
                .rebook(false)
                .createdAt(OffsetDateTime.parse("2026-11-13T08:00:00Z"))
                .build();
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(messageRepository.findByBooking_IdAndClientMessageId(5L, "c1")).thenReturn(Optional.of(existing));

        var view = service.ownerSend(42L, "5", ChatSendRequest.builder()
                .clientMessageId("c1")
                .kind("text")
                .text("salam")
                .build());

        assertEquals("7", view.getServerId());
        verify(messageRepository, never()).save(any());
        verify(chatLiveService, never()).publishNewMessageAfterCommit(any(), any());
        verify(bookingPushService, never()).chatNewMessage(any(), any(), any());
    }

    @Test
    void staffSendPushesOnlyWhenCustomerIsNotOnTheLiveLine() {
        Branch branch = Branch.builder().id(9L).name("Hyper Xəqani").build();
        Booking booking = pendingBooking(branch);
        BookingStaff staff = BookingStaff.builder().id(1L).userId(8L).build();
        when(bookingStaffAccess.requireStaff(8L, false, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 9L, "az")).thenReturn(branch);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(messageRepository.findByBooking_IdAndClientMessageId(5L, "b1")).thenReturn(Optional.empty());
        when(messageRepository.save(any(BookingMessage.class))).thenAnswer(inv -> {
            BookingMessage row = inv.getArgument(0);
            row.setId(11L);
            return row;
        });
        when(chatLiveService.hasPeerListener(5L, "branch")).thenReturn(false);

        service.staffSend(8L, false, 5L, text("b1", "Salam"), "az");

        ArgumentCaptor<BookingMessage> saved = ArgumentCaptor.forClass(BookingMessage.class);
        verify(messageRepository).save(saved.capture());
        assertEquals("sent", saved.getValue().getDelivery());
        verify(bookingPushService).chatNewMessage(booking, "text", "Salam");
    }

    @Test
    void staffSendDoesNotPushWhenCustomerIsListening() {
        Branch branch = Branch.builder().id(9L).name("Hyper Xəqani").build();
        Booking booking = pendingBooking(branch);
        BookingStaff staff = BookingStaff.builder().id(1L).userId(8L).build();
        when(bookingStaffAccess.requireStaff(8L, false, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 9L, "az")).thenReturn(branch);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(messageRepository.findByBooking_IdAndClientMessageId(5L, "b1")).thenReturn(Optional.empty());
        when(messageRepository.save(any(BookingMessage.class))).thenAnswer(inv -> {
            BookingMessage row = inv.getArgument(0);
            row.setId(11L);
            return row;
        });
        when(chatLiveService.hasPeerListener(5L, "branch")).thenReturn(true);

        service.staffSend(8L, false, 5L, text("b1", "Salam"), "az");

        ArgumentCaptor<BookingMessage> saved = ArgumentCaptor.forClass(BookingMessage.class);
        verify(messageRepository).save(saved.capture());
        assertEquals("delivered", saved.getValue().getDelivery());
        verify(bookingPushService, never()).chatNewMessage(any(), any(), any());
    }

    @Test
    void ownerReadMarksBranchMessagesSeenAndLeavesOwnMessages() {
        Booking booking = pendingBooking(Branch.builder().id(9L).build());
        booking.setCustomerUserId(42L);
        BookingMessage branchMessage = BookingMessage.builder()
                .id(1L)
                .booking(booking)
                .clientMessageId("b1")
                .fromRole("branch")
                .kind("text")
                .body("salam")
                .delivery("sent")
                .build();
        BookingMessage ownMessage = BookingMessage.builder()
                .id(2L)
                .booking(booking)
                .clientMessageId("c1")
                .fromRole("customer")
                .kind("text")
                .body("ozum")
                .delivery("sent")
                .build();
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(messageRepository.findByBooking_IdAndClientMessageIdIn(5L, List.of("b1", "c1")))
                .thenReturn(List.of(branchMessage, ownMessage));

        service.ownerRead(42L, "5", ChatReadRequest.builder().clientMessageIds(List.of("b1", "c1")).build());

        assertEquals("seen", branchMessage.getDelivery());
        assertEquals("sent", ownMessage.getDelivery());
    }

    @Test
    void audioMessageUsesProbeDurationWhenClientDisagrees() {
        Booking booking = Booking.builder().id(5L).status("pending").customerUserId(42L).build();
        BookingChatUpload upload = BookingChatUpload.builder()
                .publicId("upl_audio")
                .kind("audio")
                .fileName("6f1c9a20-1111-4111-8111-111111111111.m4a")
                .contentType("audio/mp4")
                .sizeBytes(1200L)
                .durationMs(12_000L)
                .build();
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(messageRepository.findByBooking_IdAndClientMessageId(5L, "c-audio")).thenReturn(Optional.empty());
        when(uploadRepository.findByBooking_IdAndPublicId(5L, "upl_audio")).thenReturn(Optional.of(upload));
        when(messageRepository.save(any(BookingMessage.class))).thenAnswer(inv -> {
            BookingMessage row = inv.getArgument(0);
            row.setId(21L);
            return row;
        });
        when(chatLiveService.hasPeerListener(5L, "customer")).thenReturn(true);

        var view = service.ownerSend(42L, "5", ChatSendRequest.builder()
                .clientMessageId("c-audio")
                .kind("audio")
                .uploadId("upl_audio")
                .fileName("voice-from-user.mp3")
                .durationMs(1_000L)
                .build());

        ArgumentCaptor<BookingMessage> saved = ArgumentCaptor.forClass(BookingMessage.class);
        verify(messageRepository).save(saved.capture());
        assertEquals("audio", saved.getValue().getKind());
        assertEquals(12_000L, saved.getValue().getDurationMs());
        assertEquals("6f1c9a20-1111-4111-8111-111111111111.m4a", saved.getValue().getFileName());
        assertEquals("audio/mp4", saved.getValue().getContentType());
        ChatAttachmentView attachment = (ChatAttachmentView) view.getAttachment();
        assertEquals(12_000L, attachment.getDurationMs());
        assertEquals("audio/mp4", attachment.getMime());
        assertEquals("audio", attachment.getKind());
        verify(chatLiveService).publishNewMessageAfterCommit(org.mockito.ArgumentMatchers.eq(5L), any());
    }

    @Test
    void closedConversationRejectsSend() {
        Booking booking = Booking.builder().id(5L).status("confirmed").customerUserId(42L).build();
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThrows(ConflictException.class, () -> service.ownerSend(42L, "5", ChatSendRequest.builder()
                .clientMessageId("c1")
                .kind("text")
                .text("x")
                .build()));
    }

    private static Booking pendingBooking(Branch branch) {
        return Booking.builder()
                .id(5L)
                .status("pending")
                .customerUserId(42L)
                .branch(branch)
                .build();
    }

    private static ChatSendRequest text(String clientMessageId, String text) {
        return ChatSendRequest.builder()
                .clientMessageId(clientMessageId)
                .kind("text")
                .text(text)
                .build();
    }

    @Test
    void stampUtcUsesZulu() {
        OffsetDateTime at = OffsetDateTime.parse("2026-11-13T12:01:02.345+04:00");
        assertEquals("2026-11-13T08:01:02.345Z", BookingChatService.stampUtc(at));
    }
}
