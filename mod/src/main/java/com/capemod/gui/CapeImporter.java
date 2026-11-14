package com.capemod.gui;

import com.capemod.cache.CapeLibrary;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.minecraft.client.Minecraft;

/** Abre o seletor de arquivos do sistema em thread separada e importa o PNG escolhido. */
public final class CapeImporter {
    private static volatile boolean busy;

    private CapeImporter() {}

    public static boolean isBusy() { return busy; }

    public static void begin() {
        if (busy) return;
        busy = true;
        MenuState.busy("Escolha um arquivo PNG na janela do sistema...");
        Thread t = new Thread(new Runnable() {
            public void run() {
                String error = null;
                String imported = null;
                try {
                    File chosen = choose();
                    if (chosen != null) imported = CapeLibrary.importFile(chosen);
                } catch (IOException ex) {
                    error = ex.getMessage();
                } catch (Throwable ex) {
                    error = "Não foi possível abrir o seletor de arquivos: " + ex;
                }
                final String fError = error, fImported = imported;
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    public void run() {
                        busy = false;
                        if (fError != null) MenuState.error(fError);
                        else if (fImported == null) MenuState.info("Seleção cancelada.");
                        else {
                            MenuState.previewCape = fImported;
                            MenuState.success("Capa importada: " + fImported
                                    + ". O envio ao servidor virá nas ETAPAS 3 e 4.");
                        }
                    }
                });
            }
        }, "CapeMod-FilePicker");
        t.setDaemon(true);
        t.start();
    }

    private static File choose() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        JFrame parent = new JFrame();
        parent.setAlwaysOnTop(true);
        try {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Escolha a capa (PNG)");
            fc.setFileFilter(new FileNameExtensionFilter("Imagens PNG (*.png)", "png"));
            return fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
        } finally {
            parent.dispose();
        }
    }
}
