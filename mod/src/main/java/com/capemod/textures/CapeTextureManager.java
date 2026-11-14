package com.capemod.textures;

import com.capemod.cache.CapeLibrary;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

/**
 * Carrega capas da biblioteca local como texturas do OpenGL.
 * O arquivo e lido em thread separada; a textura e criada na thread principal (exigencia do OpenGL).
 */
public final class CapeTextureManager {
    public static final class Entry {
        public final ResourceLocation location;
        public final int width;
        public final int height;
        Entry(ResourceLocation location, int width, int height) {
            this.location = location; this.width = width; this.height = height;
        }
    }

    private static final Map<String, Entry> LOADED = new HashMap<String, Entry>();
    private static final Set<String> LOADING = new HashSet<String>();
    private static final Set<String> FAILED = new HashSet<String>();
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "CapeMod-TextureLoader");
            t.setDaemon(true);
            return t;
        }
    });

    private CapeTextureManager() {}

    /** Chamar da thread principal. Retorna null enquanto carrega (ou se falhou). */
    public static synchronized Entry get(final String name) {
        Entry e = LOADED.get(name);
        if (e != null) return e;
        if (LOADING.contains(name) || FAILED.contains(name)) return null;
        final File file = CapeLibrary.fileFor(name);
        if (file == null || !file.isFile()) { FAILED.add(name); return null; }
        LOADING.add(name);
        LOADER.submit(new Runnable() {
            public void run() {
                try {
                    final BufferedImage img = ImageIO.read(file);
                    if (img == null) throw new IOException("imagem nula");
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        public void run() { register(name, img); }
                    });
                } catch (Exception ex) {
                    fail(name);
                }
            }
        });
        return null;
    }

    public static synchronized boolean hasFailed(String name) { return FAILED.contains(name); }

    private static synchronized void register(String name, BufferedImage img) {
        DynamicTexture tex = new DynamicTexture(img);
        ResourceLocation loc = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("capemod_" + name.replace(".png", ""), tex);
        LOADED.put(name, new Entry(loc, img.getWidth(), img.getHeight()));
        LOADING.remove(name);
    }

    private static synchronized void fail(String name) {
        LOADING.remove(name);
        FAILED.add(name);
    }
}
