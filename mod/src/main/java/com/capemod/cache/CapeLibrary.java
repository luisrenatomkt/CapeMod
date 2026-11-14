package com.capemod.cache;

import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.Minecraft;

/**
 * Biblioteca local de capas (.minecraft/capemod/library).
 * Os nomes de arquivo sao sempre gerados por este codigo (nunca usa o nome bruto do usuario),
 * o que impede path traversal. A validacao rigorosa de PNG sera ampliada na ETAPA 3.
 */
public final class CapeLibrary {
    public static final long MAX_BYTES = 1024L * 1024L;
    public static final int MAX_WIDTH = 1024;
    private static final Pattern NAME = Pattern.compile("^[a-z0-9_\\-]{1,40}\\.png$");

    private CapeLibrary() {}

    private static File dir() {
        File d = new File(Minecraft.getMinecraft().mcDataDir, "capemod/library");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** Retorna o arquivo da biblioteca, ou null se o nome for invalido. */
    public static File fileFor(String name) {
        return isValidName(name) ? new File(dir(), name) : null;
    }

    public static List<String> list() {
        List<String> out = new ArrayList<String>();
        String[] names = dir().list();
        if (names != null) {
            for (String n : names) if (isValidName(n)) out.add(n);
        }
        Collections.sort(out);
        return out;
    }

    /** Valida (basico) e copia um PNG para a biblioteca. Retorna o nome gerado. */
    public static String importFile(File src) throws IOException {
        if (src == null || !src.isFile()) throw new IOException("Arquivo não encontrado.");
        if (!src.getName().toLowerCase().endsWith(".png")) throw new IOException("O arquivo precisa ser .png.");
        if (src.length() > MAX_BYTES) throw new IOException("Arquivo maior que 1 MB.");

        BufferedImage img = readChecked(src);
        String name = uniqueName(sanitize(src.getName()));
        File target = new File(dir(), name);
        // Re-codifica o PNG: descarta chunks extras/metadados do arquivo original.
        if (!ImageIO.write(img, "png", target)) throw new IOException("Falha ao gravar o PNG.");
        return name;
    }

    private static BufferedImage readChecked(File src) throws IOException {
        ImageInputStream in = ImageIO.createImageInputStream(src);
        if (in == null) throw new IOException("Não foi possível ler o arquivo.");
        try {
            Iterator<ImageReader> it = ImageIO.getImageReaders(in);
            if (!it.hasNext()) throw new IOException("Arquivo não é uma imagem válida.");
            ImageReader reader = it.next();
            try {
                if (!"png".equalsIgnoreCase(reader.getFormatName())) throw new IOException("O arquivo não é um PNG.");
                reader.setInput(in, true, true);
                int w = reader.getWidth(0), h = reader.getHeight(0); // le so o cabecalho
                if (w < 64 || w > MAX_WIDTH || w % 64 != 0 || h != w / 2) {
                    throw new IOException("Tamanho inválido (" + w + "x" + h + "). Use 64x32 ou múltiplos (128x64...).");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } finally {
            in.close();
        }
    }

    private static String sanitize(String fileName) {
        String base = fileName.toLowerCase();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = base.replaceAll("[^a-z0-9_\\-]", "_");
        if (base.length() > 30) base = base.substring(0, 30);
        return base.isEmpty() ? "cape" : base;
    }

    private static String uniqueName(String base) {
        String name = base + ".png";
        int i = 2;
        while (new File(dir(), name).exists()) name = base + "_" + (i++) + ".png";
        return name;
    }
}
