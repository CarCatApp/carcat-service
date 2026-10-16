package com.carland.carland_service.service;

import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.RangeStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingSystemNotesTest {

    @Test
    void systemCodeFollowsTheLanguage() {
        assertEquals("Xidmət təqvimi silindi", BookingSystemNotes.resolve("calendar_deleted", "x", "az"));
        assertEquals("The service calendar was deleted",
                BookingSystemNotes.resolve("calendar_deleted", "x", "en-US,en;q=0.9"));
        assertEquals("Календарь услуг был удалён", BookingSystemNotes.resolve("calendar_deleted", "x", "ru"));
        assertEquals("Xidmət təqvimi silindi", BookingSystemNotes.resolve("calendar_deleted", "x", null));
        assertEquals("Xidmət təqvimi silindi", BookingSystemNotes.resolve("calendar_deleted", "x", "tr"));
        assertEquals("We were unable to accept your reservation because all places are taken.",
                BookingCapacityService.note("en"));
    }

    @Test
    void staffTypedNoteIsReturnedAsIs() {
        assertEquals("Usta xəstədir", BookingSystemNotes.resolve(null, "Usta xəstədir", "en"));
        assertEquals("Usta xəstədir", BookingSystemNotes.resolve("busy_master", "Usta xəstədir", "ru"));
        assertNull(BookingSystemNotes.text("busy_master", "en"));
    }

    @Test
    void deletedHourIsNeverOffered() {
        assertTrue(SlotOffer.hidden(Range.builder().status(RangeStatus.DELETED.name()).build()));
    }
}
