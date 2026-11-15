package com.capemod.gui;

import com.capemod.configuration.CapeConfig;
import com.capemod.initialization.CapeMod;
import com.capemod.networking.ConnectionStatus;
import com.capemod.textures.CapeTextureManager;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Menu principal do CapeMod (tecla C). */
public class CapeMenuScreen extends GuiScreen {
    static final int PANEL_W = 300, PANEL_H = 210;
    private static final int BTN_GALLERY = 0, BTN_UPLOAD = 1, BTN_SELECT = 2, BTN_REMOVE = 3, BTN_CLOSE = 4;

    private GuiButton uploadButton, selectButton, removeButton;
    private int px, py;

    @Override
    public void initGui() {
        MenuState.ensurePreview();
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        buttonList.clear();
        int bx = px + 10;
        buttonList.add(new GuiButton(BTN_GALLERY, bx, py + 34, 120, 20, "Galeria de capas"));
        uploadButton = new GuiButton(BTN_UPLOAD, bx, py + 58, 120, 20, "Enviar capa PNG");
        selectButton = new GuiButton(BTN_SELECT, bx, py + 82, 120, 20, "Selecionar capa");
        removeButton = new GuiButton(BTN_REMOVE, bx, py + 106, 120, 20, "Remover capa atual");
        buttonList.add(uploadButton);
        buttonList.add(selectButton);
        buttonList.add(removeButton);
        buttonList.add(new GuiButton(BTN_CLOSE, bx, py + 140, 120, 20, "Fechar"));
        updateButtons();
    }

    private void updateButtons() {
        String preview = MenuState.previewCape;
        String active = CapeConfig.getActiveCape();
        uploadButton.enabled = !CapeImporter.isBusy();
        selectButton.enabled = preview != null && !preview.equals(active);
        removeButton.enabled = !active.isEmpty();
    }

    @Override
    public void updateScreen() { updateButtons(); }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        switch (b.id) {
            case BTN_GALLERY:
                mc.displayGuiScreen(new CapeGalleryScreen(this));
                break;
            case BTN_UPLOAD:
                CapeImporter.begin();
                break;
            case BTN_SELECT:
                CapeConfig.setActiveCape(MenuState.previewCape);
                MenuState.success("Capa selecionada: " + MenuState.previewCape
                        + " (a exibição no personagem vem na ETAPA 7).");
                break;
            case BTN_REMOVE:
                CapeConfig.setActiveCape(null);
                MenuState.info("Capa atual removida.");
                break;
            case BTN_CLOSE:
                mc.displayGuiScreen(null);
                break;
            default:
                break;
        }
        updateButtons();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == CapeMod.getMenuKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawPanel(px, py);

        drawString(fontRendererObj, "CapeMod", px + 10, py + 10, 0xFFFFFF);
        ConnectionStatus st = ConnectionStatus.current();
        int tw = fontRendererObj.getStringWidth(st.label);
        Gui.drawRect(px + PANEL_W - tw - 22, py + 11, px + PANEL_W - tw - 16, py + 17, st.color);
        drawString(fontRendererObj, st.label, px + PANEL_W - tw - 10, py + 10, 0xCCCCCC);

        // Area de previa
        Gui.drawRect(px + 140, py + 28, px + 290, py + 176, 0x80000000);
        int cx = px + 215;
        String name = MenuState.previewCape;
        if (name == null) {
            drawCenteredString(fontRendererObj, "Nenhuma capa", cx, py + 98, 0xAAAAAA);
        } else {
            drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(name, 144), cx, py + 32, 0xFFFFFF);
            CapeTextureManager.Entry e = CapeTextureManager.get(name);
            if (e == null) {
                String msg = CapeTextureManager.hasFailed(name) ? "Falha ao carregar" : "Carregando...";
                drawCenteredString(fontRendererObj, msg, cx, py + 98, 0xAAAAAA);
            } else {
                CapePreviewRenderer.drawFace(e, cx - 25, py + 44, 50, 80);
                CapePreviewRenderer.drawFull(e, cx - 48, py + 128, 96, 48);
                drawCenteredString(fontRendererObj, e.width + "x" + e.height, cx, py + 118 + 10 - 4, 0x888888);
            }
        }

        float prog = MenuState.progress;
        if (prog >= 0F) {
            int bx = px + 10, by = py + 168;
            Gui.drawRect(bx, by, bx + 120, by + 6, 0xFF303040);
            Gui.drawRect(bx, by, bx + (int) (120 * Math.min(1F, prog)), by + 6, 0xFF55FF55);
        }

        // Mensagem de status (ate 2 linhas)
        String msg = MenuState.currentMessage();
        if (!msg.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<String> lines = fontRendererObj.listFormattedStringToWidth(msg, PANEL_W - 20);
            for (int i = 0; i < lines.size() && i < 2; i++) {
                drawString(fontRendererObj, lines.get(i), px + 10, py + 182 + i * 10, MenuState.currentColor());
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    static void drawPanel(int x, int y) {
        Gui.drawRect(x, y, x + PANEL_W, y + PANEL_H, 0xE0101018);
        int c = 0xFF5A5A78;
        Gui.drawRect(x, y, x + PANEL_W, y + 1, c);
        Gui.drawRect(x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, c);
        Gui.drawRect(x, y, x + 1, y + PANEL_H, c);
        Gui.drawRect(x + PANEL_W - 1, y, x + PANEL_W, y + PANEL_H, c);
    }
}
