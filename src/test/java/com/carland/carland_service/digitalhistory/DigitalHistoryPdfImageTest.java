package com.carland.carland_service.digitalhistory;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void brokenWebpDoesNotThrow() {
        byte[] riff = new byte[] {
                'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0
        };
        assertNull(DigitalHistoryPdf.pdfImage(riff));
    }
}
