package com.capemod.gui;

import com.capemod.textures.CapeTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/** Desenha previas 2D de capas. Face externa da capa vanilla 64x32: u=1..11, v=1..17. */
public final class CapePreviewRenderer {
    private CapePreviewRenderer() {}

    private static void begin(CapeTextureManager.Entry e) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(e.location);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Face externa da capa (vista de costas), escalada para w x h. */
    public static void drawFace(CapeTextureManager.Entry e, int x, int y, int w, int h) {
        begin(e);
        float s = e.width / 64F;
        Gui.drawScaledCustomSizeModalRect(x, y, s, s, (int) (10 * s), (int) (16 * s), w, h, e.width, e.height);
        GlStateManager.disableBlend();
    }

    /** Textura inteira, escalada para w x h. */
    public static void drawFull(CapeTextureManager.Entry e, int x, int y, int w, int h) {
        begin(e);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, e.width, e.height, w, h, e.width, e.height);
        GlStateManager.disableBlend();
    }
}
