package com.capemod.textures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.Test;

public class CapeValidatorTest {

    private static byte[] png(int w, int h, int argb) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setRGB(x, y, argb);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test public void acceptsOpaque64x32() throws IOException {
        CapeValidator.Result r = CapeValidator.validate(png(64, 32, 0xFFAA2222));
        assertTrue(r.error, r.ok);
        assertEquals(64, r.width);
        assertEquals(32, r.height);
    }

    @Test public void acceptsHd128x64() throws IOException {
        assertTrue(CapeValidator.validate(png(128, 64, 0xFF2222AA)).ok);
    }

    @Test public void rejectsWrongRatio() throws IOException {
        assertFalse(CapeValidator.validate(png(64, 64, 0xFFFFFFFF)).ok);
    }

    @Test public void rejectsNonPowerOfTwoWidth() throws IOException {
        assertFalse(CapeValidator.validate(png(96, 48, 0xFFFFFFFF)).ok);
    }

    @Test public void rejectsTooLarge() throws IOException {
        assertFalse(CapeValidator.validate(png(2048, 1024, 0xFFFFFFFF)).ok);
    }

    @Test public void rejectsFullyTransparent() throws IOException {
        CapeValidator.Result r = CapeValidator.validate(png(64, 32, 0x00000000));
        assertFalse(r.ok);
    }

    @Test public void warnsOnSemiTransparent() throws IOException {
        CapeValidator.Result r = CapeValidator.validate(png(64, 32, 0x80FF0000));
        assertTrue(r.ok);
        assertFalse(r.warnings.isEmpty());
    }

    @Test public void rejectsGarbage() {
        assertFalse(CapeValidator.validate("isto nao e um png, mas e longo o bastante".getBytes()).ok);
        assertFalse(CapeValidator.validate(new byte[0]).ok);
        assertFalse(CapeValidator.validate((byte[]) null).ok);
    }

    @Test public void rejectsJpegRenamed() throws IOException {
        BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        assertFalse(CapeValidator.validate(out.toByteArray()).ok);
    }

    @Test public void rejectsTruncatedPng() throws IOException {
        byte[] d = png(64, 32, 0xFFAA2222);
        assertFalse(CapeValidator.validate(Arrays.copyOf(d, d.length - 12)).ok);
    }

    @Test public void rejectsBadCrc() throws IOException {
        byte[] d = png(64, 32, 0xFFAA2222);
        d[32] ^= 0x55; // ultimo byte do CRC do IHDR
        CapeValidator.Result r = CapeValidator.validate(d);
        assertFalse(r.ok);
        assertTrue(r.error, r.error.contains("CRC"));
    }

    @Test public void rejectsTrailingData() throws IOException {
        byte[] d = png(64, 32, 0xFFAA2222);
        byte[] e = Arrays.copyOf(d, d.length + 4);
        assertFalse(CapeValidator.validate(e).ok);
    }

    @Test public void rejectsOversizedInput() {
        assertFalse(CapeValidator.validate(new byte[CapeValidator.MAX_BYTES + 1]).ok);
    }

    @Test public void encodeProducesValidPng() throws IOException {
        CapeValidator.Result r = CapeValidator.validate(png(64, 32, 0xFF22AA22));
        assertTrue(CapeValidator.validate(CapeValidator.encode(r.image)).ok);
    }
}
