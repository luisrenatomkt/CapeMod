package com.capemod.server.security;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

/**
 * Validacao rigorosa de PNG de capa no SERVIDOR (copia independente da do mod: o servidor nunca confia no cliente).
 * Ordem: tamanho -> assinatura -> estrutura/CRC de todos os chunks -> dimensoes (so pelo cabecalho,
 * antes de decodificar, contra "bombas de descompressao") -> decodificacao -> transparencia.
 * O servidor (ETAPA 4) deve repetir estas regras: nunca confiar na validacao do cliente.
 */
public final class PngValidator {
    public static final int MAX_BYTES = 1024 * 1024;
    public static final int MIN_WIDTH = 64;
    public static final int MAX_WIDTH = 1024;
    private static final int MAX_CHUNKS = 256;
    private static final double MIN_VISIBLE_FACE = 0.5;
    private static final byte[] SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private PngValidator() {}

    public static final class Result {
        public final boolean ok;
        public final String error;
        public final List<String> warnings;
        public final BufferedImage image; // ARGB, apenas se ok
        public final int width, height;

        private Result(boolean ok, String error, List<String> warnings, BufferedImage image) {
            this.ok = ok; this.error = error; this.warnings = warnings; this.image = image;
            this.width = image == null ? 0 : image.getWidth();
            this.height = image == null ? 0 : image.getHeight();
        }
        static Result fail(String error) {
            return new Result(false, error, Collections.<String>emptyList(), null);
        }
        static Result ok(BufferedImage img, List<String> warnings) {
            return new Result(true, null, warnings, img);
        }
    }

    private static final class Invalid extends Exception {
        Invalid(String m) { super(m); }
    }

    public static boolean isAllowedSize(long w, long h) {
        return w >= MIN_WIDTH && w <= MAX_WIDTH && (w & (w - 1)) == 0 && h == w / 2;
    }

    public static Result validate(File f) {
        if (f == null || !f.isFile()) return Result.fail("Arquivo não encontrado.");
        if (!f.getName().toLowerCase().endsWith(".png")) return Result.fail("O arquivo precisa ter extensão .png.");
        if (f.length() > MAX_BYTES) return Result.fail("Arquivo maior que 1 MB.");
        try {
            return validate(Files.readAllBytes(f.toPath()));
        } catch (IOException ex) {
            return Result.fail("Não foi possível ler o arquivo.");
        }
    }

    public static Result validate(byte[] d) {
        try {
            if (d == null || d.length == 0) throw new Invalid("Arquivo vazio.");
            if (d.length > MAX_BYTES) throw new Invalid("Arquivo maior que 1 MB.");
            int[] size = checkStructure(d);
            int w = size[0], h = size[1];
            if (!isAllowedSize(w, h)) {
                throw new Invalid("Tamanho inválido (" + w + "x" + h + "). Use 64x32 ou o dobro "
                        + "sucessivamente (128x64, 256x128, 512x256, 1024x512).");
            }
            BufferedImage src;
            try {
                src = ImageIO.read(new ByteArrayInputStream(d));
            } catch (IOException ex) {
                throw new Invalid("Não foi possível decodificar o PNG.");
            } catch (RuntimeException ex) {
                throw new Invalid("Não foi possível decodificar o PNG.");
            }
            if (src == null || src.getWidth() != w || src.getHeight() != h) {
                throw new Invalid("Imagem inconsistente com o cabeçalho do PNG.");
            }
            int[] px = src.getRGB(0, 0, w, h, null, 0, w);
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, w, h, px, 0, w);
            List<String> warnings = new ArrayList<String>();
            analyzeTransparency(px, w, h, warnings);
            return Result.ok(img, warnings);
        } catch (Invalid ex) {
            return Result.fail(ex.getMessage());
        }
    }

    /** Re-codifica como PNG limpo (sem metadados nem chunks extras). */
    public static byte[] encode(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(img, "png", out)) throw new IOException("Falha ao codificar o PNG.");
        return out.toByteArray();
    }

    private static long readUInt(byte[] d, int o) {
        return ((long) (d[o] & 0xFF) << 24) | ((d[o + 1] & 0xFF) << 16) | ((d[o + 2] & 0xFF) << 8) | (d[o + 3] & 0xFF);
    }

    /** Percorre todos os chunks verificando CRC e ordem. Retorna {largura, altura}. */
    private static int[] checkStructure(byte[] d) throws Invalid {
        if (d.length < 8 + 12 + 13) throw new Invalid("O arquivo não é um PNG válido.");
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (d[i] != SIGNATURE[i]) throw new Invalid("O arquivo não é um PNG (assinatura inválida).");
        }
        int pos = 8, count = 0, w = 0, h = 0;
        boolean sawIdat = false;
        CRC32 crc = new CRC32();
        while (true) {
            if (pos + 12 > d.length) throw new Invalid("PNG incompleto ou truncado.");
            long len = readUInt(d, pos);
            if (len > d.length - pos - 12) throw new Invalid("PNG corrompido (chunk maior que o arquivo).");
            String type = new String(d, pos + 4, 4, StandardCharsets.ISO_8859_1);
            crc.reset();
            crc.update(d, pos + 4, 4 + (int) len);
            if (crc.getValue() != readUInt(d, pos + 8 + (int) len)) {
                throw new Invalid("PNG corrompido (CRC inválido no chunk " + type + ").");
            }
            if (++count > MAX_CHUNKS) throw new Invalid("PNG com chunks demais.");
            if (count == 1 && !type.equals("IHDR")) throw new Invalid("PNG inválido (IHDR ausente).");

            if (type.equals("IHDR")) {
                if (count != 1 || len != 13) throw new Invalid("PNG inválido (cabeçalho IHDR).");
                long lw = readUInt(d, pos + 8), lh = readUInt(d, pos + 12);
                if (lw > 100000 || lh > 100000) throw new Invalid("Tamanho inválido (" + lw + "x" + lh + ").");
                w = (int) lw; h = (int) lh;
            } else if (type.equals("IDAT")) {
                sawIdat = true;
            } else if (type.equals("acTL") || type.equals("fcTL") || type.equals("fdAT")) {
                throw new Invalid("PNG animado (APNG) não é suportado.");
            } else if (type.equals("IEND")) {
                if (len != 0) throw new Invalid("PNG inválido (IEND).");
                if (pos + 12 != d.length) throw new Invalid("Há dados extras após o fim do PNG.");
                if (!sawIdat) throw new Invalid("PNG sem dados de imagem.");
                return new int[]{w, h};
            } else if (Character.isUpperCase(type.charAt(0)) && !type.equals("PLTE")) {
                throw new Invalid("PNG com chunk crítico desconhecido (" + type + ").");
            }
            pos += 12 + (int) len;
        }
    }

    /**
     * Face externa da capa vanilla 64x32: x=1..11, y=1..17 (escalada em texturas maiores).
     * Rejeita capa invisivel; avisa sobre pixels semitransparentes.
     */
    private static void analyzeTransparency(int[] px, int w, int h, List<String> warnings) throws Invalid {
        float s = w / 64f;
        int x0 = (int) (1 * s), x1 = (int) (11 * s), y0 = (int) (1 * s), y1 = (int) (17 * s);
        int area = (x1 - x0) * (y1 - y0), visible = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (((px[y * w + x] >>> 24) & 0xFF) != 0) visible++;
            }
        }
        if (visible < area * MIN_VISIBLE_FACE) {
            throw new Invalid("A área principal da capa tem só " + (100 * visible / area)
                    + "% de pixels visíveis (mínimo 50%). A imagem está transparente demais.");
        }
        int semi = 0;
        for (int p : px) {
            int a = (p >>> 24) & 0xFF;
            if (a > 0 && a < 255) semi++;
        }
        if (semi > 0) {
            warnings.add(semi + " pixels semitransparentes; podem aparecer com falhas no jogo.");
        }
    }
}
