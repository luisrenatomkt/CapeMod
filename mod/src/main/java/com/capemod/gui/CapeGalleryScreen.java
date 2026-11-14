package com.capemod.gui;

import com.capemod.cache.CapeLibrary;
import com.capemod.configuration.CapeConfig;
import com.capemod.textures.CapeTextureManager;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

/** Galeria da biblioteca local: clique numa capa para coloca-la em previa. */
public class CapeGalleryScreen extends GuiScreen {
    private static final int ROWS = 4, ROW_H = 30;
    private static final int BTN_BACK = 0, BTN_PREV = 1, BTN_NEXT = 2;

    private final GuiScreen parent;
    private List<String> items;
    private int page, px, py;
    private GuiButton prev, next;

    public CapeGalleryScreen(GuiScreen parent) { this.parent = parent; }

    private int pages() { return Math.max(1, (items.size() + ROWS - 1) / ROWS); }

    @Override
    public void initGui() {
        items = CapeLibrary.list();
        px = (width - CapeMenuScreen.PANEL_W) / 2;
        py = (height - CapeMenuScreen.PANEL_H) / 2;
        page = Math.min(page, pages() - 1);
        buttonList.clear();
        prev = new GuiButton(BTN_PREV, px + 10, py + 156, 40, 20, "<");
        next = new GuiButton(BTN_NEXT, px + CapeMenuScreen.PANEL_W - 50, py + 156, 40, 20, ">");
        buttonList.add(prev);
        buttonList.add(next);
        buttonList.add(new GuiButton(BTN_BACK, px + 100, py + 182, 100, 20, "Voltar"));
        updatePager();
    }

    private void updatePager() {
        prev.enabled = page > 0;
        next.enabled = page < pages() - 1;
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == BTN_BACK) mc.displayGuiScreen(parent);
        else if (b.id == BTN_PREV && page > 0) page--;
        else if (b.id == BTN_NEXT && page < pages() - 1) page++;
        updatePager();
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            if (idx >= items.size()) break;
            int y = py + 30 + i * ROW_H;
            if (mx >= px + 10 && mx < px + 290 && my >= y && my < y + ROW_H - 2) {
                MenuState.previewCape = items.get(idx);
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        CapeMenuScreen.drawPanel(px, py);
        drawString(fontRendererObj, "Galeria de capas", px + 10, py + 10, 0xFFFFFF);

        if (items.isEmpty()) {
            drawCenteredString(fontRendererObj, "Nenhuma capa na biblioteca.", px + 150, py + 80, 0xAAAAAA);
            drawCenteredString(fontRendererObj, "Use \"Enviar capa PNG\" no menu.", px + 150, py + 94, 0x888888);
        }
        String active = CapeConfig.getActiveCape();
        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            if (idx >= items.size()) break;
            String name = items.get(idx);
            int y = py + 30 + i * ROW_H;
            boolean hover = mouseX >= px + 10 && mouseX < px + 290 && mouseY >= y && mouseY < y + ROW_H - 2;
            int bg = name.equals(MenuState.previewCape) ? 0x80406090 : (hover ? 0x60FFFFFF : 0x40000000);
            Gui.drawRect(px + 10, y, px + 290, y + ROW_H - 2, bg);
            CapeTextureManager.Entry e = CapeTextureManager.get(name);
            if (e != null) {
                CapePreviewRenderer.drawFace(e, px + 16, y + 2, 15, 24);
                drawString(fontRendererObj, e.width + "x" + e.height, px + 40, y + 16, 0x888888);
            }
            drawString(fontRendererObj, fontRendererObj.trimStringToWidth(name, 170), px + 40, y + 5, 0xFFFFFF);
            if (name.equals(active)) {
                String tag = "ATIVA";
                drawString(fontRendererObj, tag, px + 284 - fontRendererObj.getStringWidth(tag), y + 10, 0x55FF55);
            }
        }
        drawCenteredString(fontRendererObj, (page + 1) + " / " + pages(), px + 150, py + 162, 0xCCCCCC);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
