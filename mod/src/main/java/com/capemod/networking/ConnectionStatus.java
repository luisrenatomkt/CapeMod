package com.capemod.networking;

import com.capemod.configuration.CapeConfig;

/**
 * Estado de conexao exibido no menu. Na ETAPA 2 ainda nao ha requisicoes HTTP:
 * so distingue "sem URL configurada" de "URL configurada, ainda nao verificada".
 * ONLINE/OFFLINE/CHECKING serao usados quando o cliente HTTP existir (ETAPA 4/6).
 */
public enum ConnectionStatus {
    NOT_CONFIGURED("Servidor não configurado", 0xFF888888),
    UNVERIFIED("Servidor não verificado", 0xFFE0C040),
    CHECKING("Verificando...", 0xFFE0C040),
    ONLINE("Conectado", 0xFF40C040),
    OFFLINE("Offline", 0xFFC04040);

    public final String label;
    public final int color;

    ConnectionStatus(String label, int color) { this.label = label; this.color = color; }

    public static ConnectionStatus current() {
        return CapeConfig.getApiBaseUrl().isEmpty() ? NOT_CONFIGURED : UNVERIFIED;
    }
}
