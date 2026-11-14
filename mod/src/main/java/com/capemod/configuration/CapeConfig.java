package com.capemod.configuration;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/** Configuracao persistente do CapeMod (config/capemod.cfg). */
public final class CapeConfig {
    private static Configuration config;
    private static String activeCape = "";
    private static String apiBaseUrl = "";

    private CapeConfig() {}

    public static void load(File file) {
        config = new Configuration(file);
        activeCape = config.getString("activeCape", "general", "",
                "Arquivo da capa ativa na biblioteca local");
        apiBaseUrl = config.getString("apiBaseUrl", "server", "",
                "URL base HTTPS da API (usada a partir da ETAPA 4)");
        if (config.hasChanged()) config.save();
    }

    public static String getActiveCape() { return activeCape; }

    public static String getApiBaseUrl() { return apiBaseUrl; }

    public static void setActiveCape(String name) {
        activeCape = name == null ? "" : name;
        if (config != null) {
            config.get("general", "activeCape", "").set(activeCape);
            config.save();
        }
    }
}
