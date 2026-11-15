package com.capemod.configuration;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/** Configuracao persistente do CapeMod (config/capemod.cfg). */
public final class CapeConfig {
    private static Configuration config;
    private static String activeCape = "";
    private static String apiBaseUrl = "";
    private static String authToken = "";
    private static boolean allowInsecureLocalhost = false;

    private CapeConfig() {}

    public static void load(File file) {
        config = new Configuration(file);
        activeCape = config.getString("activeCape", "general", "",
                "Arquivo da capa ativa na biblioteca local");
        apiBaseUrl = config.getString("apiBaseUrl", "server", "",
                "URL base HTTPS da API, ex.: https://seu-servidor.exemplo");
        authToken = config.getString("authToken", "server", "",
                "Token de acesso (preenchido pelo login na ETAPA 5). NAO compartilhe este arquivo.");
        allowInsecureLocalhost = config.getBoolean("allowInsecureLocalhost", "server", false,
                "SOMENTE DESENVOLVIMENTO: permite http:// apenas para localhost/127.0.0.1");
        if (config.hasChanged()) config.save();
    }

    public static String getActiveCape() { return activeCape; }
    public static String getApiBaseUrl() { return apiBaseUrl; }
    public static String getAuthToken() { return authToken; }
    public static boolean isAllowInsecureLocalhost() { return allowInsecureLocalhost; }

    public static void setActiveCape(String name) {
        activeCape = name == null ? "" : name;
        if (config != null) {
            config.get("general", "activeCape", "").set(activeCape);
            config.save();
        }
    }
}
