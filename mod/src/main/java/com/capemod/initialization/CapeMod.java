package com.capemod.initialization;

import com.capemod.configuration.CapeConfig;
import com.capemod.gui.CapeMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = CapeMod.MODID, name = CapeMod.NAME, version = CapeMod.VERSION,
        acceptedMinecraftVersions = "[1.8.9]", clientSideOnly = true)
public class CapeMod {
    public static final String MODID = "capemod";
    public static final String NAME = "CapeMod";
    public static final String VERSION = "1.0.0";

    private static KeyBinding openMenuKey;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CapeConfig.load(event.getSuggestedConfigurationFile());
        FMLLog.info("[%s] preInit", NAME);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        openMenuKey = new KeyBinding("key.capemod.menu", Keyboard.KEY_C, "key.categories.capemod");
        ClientRegistry.registerKeyBinding(openMenuKey);
        MinecraftForge.EVENT_BUS.register(this);
        FMLLog.info("[%s] init concluido", NAME);
    }

    /** Codigo atual da tecla de abrir o menu (configuravel em Opcoes > Controles). */
    public static int getMenuKeyCode() {
        return openMenuKey == null ? Keyboard.KEY_C : openMenuKey.getKeyCode();
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (openMenuKey.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null) mc.displayGuiScreen(new CapeMenuScreen());
        }
    }
}
