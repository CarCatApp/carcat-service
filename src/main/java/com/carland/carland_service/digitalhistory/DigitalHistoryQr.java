package com.carland.carland_service.digitalhistory;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * tr: Yoxlama ünvanının QR şəkli. PDF bunu basır.
 * en: QR image of the verify URL. The PDF prints it.
 */
final class DigitalHistoryQr {

    private DigitalHistoryQr() {
    }

    static byte[] png(String url) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 256, 256);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF111214 : 0xFFFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", out)) {
                throw new IllegalStateException("qr png");
            }
            return out.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("digital history qr failed", ex);
        }
    }
}
