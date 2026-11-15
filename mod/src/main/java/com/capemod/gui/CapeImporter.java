package com.capemod.gui;

import com.capemod.cache.CapeLibrary;
import com.capemod.networking.CapeUploader;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.minecraft.client.Minecraft;

/**
 * Fluxo "Enviar capa PNG": escolher arquivo -> validar/importar -> enviar ao servidor (se configurado).
 * Tudo fora da thread principal; os resultados voltam via addScheduledTask.
 */
public final class CapeImporter {
    private static volatile boolean busy;

    private CapeImporter() {}

    public static boolean isBusy() { return busy; }

    private static void onMain(Runnable r) { Minecraft.getMinecraft().addScheduledTask(r); }

    public static void begin() {
        if (busy) return;
        busy = true;
        MenuState.progress = -1F;
        MenuState.busy("Escolha um arquivo PNG na janela do sistema...");
        Thread t = new Thread(new Runnable() {
            public void run() {
                String error = null;
                CapeLibrary.ImportResult imported = null;
                try {
                    File chosen = choose();
                    if (chosen != null) imported = CapeLibrary.importFile(chosen);
                    else error = "";
                } catch (IOException ex) {
                    error = ex.getMessage();
                } catch (Throwable ex) {
                    error = "Não foi possível abrir o seletor de arquivos: " + ex;
                }
                final String fError = error;
                final CapeLibrary.ImportResult fImported = imported;
                onMain(new Runnable() {
                    public void run() { afterImport(fError, fImported); }
                });
            }
        }, "CapeMod-FilePicker");
        t.setDaemon(true);
        t.start();
    }

    /** Roda na thread principal. */
    private static void afterImport(String error, CapeLibrary.ImportResult imported) {
        if (imported == null) {
            busy = false;
            if (error == null || error.isEmpty()) MenuState.info("Seleção cancelada.");
            else MenuState.error(error);
            return;
        }
        MenuState.previewCape = imported.name;
        String warn = imported.warnings.isEmpty() ? "" : " Aviso: " + imported.warnings.get(0);

        String blocked = CapeUploader.preflight();
        if (blocked != null) {
            busy = false;
            MenuState.info("Capa salva localmente (" + imported.name + "). Não enviada: " + blocked + warn);
            return;
        }
        final byte[] data;
        try {
            data = CapeLibrary.readBytes(imported.name);
        } catch (IOException ex) {
            busy = false;
            MenuState.error(ex.getMessage());
            return;
        }
        MenuState.progress = 0F;
        MenuState.busy("Enviando " + imported.name + "...");
        CapeUploader.upload(data, new CapeUploader.Listener() {
            public void onProgress(long sent, long total) {
                MenuState.progress = total <= 0 ? 0F : (float) sent / (float) total;
            }
            public void onSuccess(final String body) {
                onMain(new Runnable() {
                    public void run() {
                        busy = false;
                        MenuState.progress = -1F;
                        MenuState.success("Capa enviada ao servidor.");
                    }
                });
            }
            public void onError(final String message) {
                onMain(new Runnable() {
                    public void run() {
                        busy = false;
                        MenuState.progress = -1F;
                        MenuState.error("Falha no envio: " + message);
                    }
                });
            }
        });
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
