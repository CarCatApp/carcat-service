package com.carland.carland_service.digitalhistory;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigitalHistoryPdfImageTest {

    @Test
    void webpCarPhotoBecomesJpeg() throws Exception {
        byte[] webp;
        try (InputStream in = getClass().getResourceAsStream("/digitalhistory/dot.webp")) {
            assertNotNull(in);
            webp = in.readAllBytes();
        }
        byte[] jpeg = DigitalHistoryPdf.pdfImage(webp);
        assertNotNull(jpeg);
        assertEquals((byte) 0xFF, jpeg[0]);
        assertEquals((byte) 0xD8, jpeg[1]);
    }

    @Test
    void tallPhotoKeepsTheRoofAndNudgesTheCards() {
        DigitalHistoryPdf.PhotoBox box = DigitalHistoryPdf.photoBox(1000f, 800f);

        assertTrue(box.h() > 135.6f);
        assertEquals(1000f / 800f, box.w() / box.h(), 0.01f);
        assertTrue(box.drop() > 0f);
        assertTrue(box.drop() <= 16f);
        assertTrue(box.y() + box.h() + 6f <= 263.2f + box.drop() + 0.1f);
    }

    @Test
    void widePhotoStaysFullWidthWithoutMovingCards() {
        DigitalHistoryPdf.PhotoBox box = DigitalHistoryPdf.photoBox(1680f, 720f);

        assertEquals(258.1f, box.w(), 0.2f);
        assertEquals(0f, box.drop(), 0.01f);
        assertTrue(box.h() < 135.6f);
    }

    @Test
    void brokenWebpDoesNotThrow() {
        byte[] riff = new byte[] {
                'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0
        };
        assertNull(DigitalHistoryPdf.pdfImage(riff));
    }
}
