package com.capemod.cache;

import com.capemod.textures.CapeValidator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;

/**
 * Biblioteca local de capas (.minecraft/capemod/library).
 * Nomes de arquivo sao sempre gerados aqui (nunca o nome bruto do usuario): sem path traversal.
 * Todo PNG importado passa por CapeValidator e e regravado limpo.
 */
public final class CapeLibrary {
    private static final Pattern NAME = Pattern.compile("^[a-z0-9_\\-]{1,40}\\.png$");

    private CapeLibrary() {}

    public static final class ImportResult {
        public final String name;
        public final List<String> warnings;
        ImportResult(String name, List<String> warnings) { this.name = name; this.warnings = warnings; }
    }

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

    public static byte[] readBytes(String name) throws IOException {
        File f = fileFor(name);
        if (f == null || !f.isFile()) throw new IOException("Capa não encontrada na biblioteca.");
        return Files.readAllBytes(f.toPath());
    }

    public static ImportResult importFile(File src) throws IOException {
        CapeValidator.Result r = CapeValidator.validate(src);
        if (!r.ok) throw new IOException(r.error);
        byte[] clean = CapeValidator.encode(r.image);
        String name = uniqueName(sanitize(src.getName()));
        Files.write(new File(dir(), name).toPath(), clean);
        return new ImportResult(name, r.warnings);
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
