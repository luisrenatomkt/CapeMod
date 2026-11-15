package com.capemod.gui;

import com.capemod.cache.CapeLibrary;
import com.capemod.configuration.CapeConfig;
import java.io.File;

/** Estado compartilhado entre as telas do CapeMod (capa em previa e mensagem de status). */
public final class MenuState {
    public static volatile String previewCape;
    /** Progresso do upload (0..1); negativo = oculto. */
    public static volatile float progress = -1F;
    private static volatile String message = "";
    private static volatile int messageColor = 0xFFFFFF;
    private static volatile long messageExpires = 0;

    private MenuState() {}

    /** Garante que a previa aponta para uma capa existente (por padrao, a ativa). */
    public static void ensurePreview() {
        String p = previewCape;
        if (p != null) {
            File f = CapeLibrary.fileFor(p);
            if (f == null || !f.isFile()) previewCape = null;
        }
        if (previewCape == null) {
            String active = CapeConfig.getActiveCape();
            File f = CapeLibrary.fileFor(active);
            if (f != null && f.isFile()) previewCape = active;
        }
    }

    public static void info(String m) { set(m, 0xFFFFFF, 8000); }
    public static void success(String m) { set(m, 0x55FF55, 8000); }
    public static void error(String m) { set(m, 0xFF5555, 10000); }
    public static void busy(String m) { set(m, 0xFFFF55, Long.MAX_VALUE / 2); }

    private static void set(String m, int color, long durationMs) {
        message = m; messageColor = color; messageExpires = System.currentTimeMillis() + durationMs;
    }

    public static String currentMessage() {
        return System.currentTimeMillis() < messageExpires ? message : "";
    }

    public static int currentColor() { return messageColor; }
}
