package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingRejectRequest;
import com.carland.carland_service.entity.BookingStaffNote;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.BookingStaffNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingStaffNoteServiceTest {

    @Mock BookingStaffNoteRepository noteRepository;
    @Mock ServiceCategoryJson json;

    BookingStaffNoteService service;

    @BeforeEach
    void setUp() {
        service = new BookingStaffNoteService(noteRepository, json);
    }

    @Test
    void manualReasonHasNoCode() {
        BookingRejectRequest body = new BookingRejectRequest();
        body.setReason("öz mətnim");

        BookingStaffNoteService.Applied applied = service.apply("cancel", body, "az");

        assertNull(applied.code());
        assertEquals("öz mətnim", applied.note());
    }

    @Test
    void catalogueReasonSnapshotsTheCurrentSentence() {
        BookingStaffNote note = BookingStaffNote.builder()
                .id(4L).kind("no_show").code("no_show_4").titleJson("{}").build();
        when(noteRepository.findById(4L)).thenReturn(Optional.of(note));
        when(json.read("{}")).thenReturn(java.util.Map.of("az", "Gəlmədi", "en", "No show"));
        BookingRejectRequest body = new BookingRejectRequest();
        body.setNoteId(4L);

        BookingStaffNoteService.Applied applied = service.apply("no_show", body, "en");

        assertEquals("no_show_4", applied.code());
        assertEquals("No show", applied.note());
    }

    @Test
    void emptyManualReasonIsRejected() {
        assertThrows(MissingFieldException.class, () -> service.apply("cancel", new BookingRejectRequest(), "az"));
    }
}
