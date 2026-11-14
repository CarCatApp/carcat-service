package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.ChatSendRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingMessage;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.repository.BookingMessageRepository;
import com.carland.carland_service.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
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
    @Mock BookingStaffAccess bookingStaffAccess;
    @Mock BookingChatLiveService chatLiveService;
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

    @Test
    void stampUtcUsesZulu() {
        OffsetDateTime at = OffsetDateTime.parse("2026-11-13T12:01:02.345+04:00");
        assertEquals("2026-11-13T08:01:02.345Z", BookingChatService.stampUtc(at));
    }
}
