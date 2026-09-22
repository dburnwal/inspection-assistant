package com.dburnwal.inspectionassistant.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Resizes images before sending to the AI model.
 * Keeps aspect ratio. Configurable max dimensions.
 */
@Component
public class ImageResizer {

    @Value("${inspection.image.max-width:1280}")
    private int maxWidth;

    @Value("${inspection.image.max-height:720}")
    private int maxHeight;

    public byte[] resize(byte[] imageBytes) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) return imageBytes;

            int w = original.getWidth();
            int h = original.getHeight();
            if (w <= maxWidth && h <= maxHeight) return imageBytes;

            double scale = Math.min((double) maxWidth / w, (double) maxHeight / h);
            int newW = (int) (w * scale);
            int newH = (int) (h * scale);

            BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, newW, newH, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", out);
            return out.toByteArray();
        } catch (Exception e) {
            return imageBytes; // return original on failure
        }
    }
}
