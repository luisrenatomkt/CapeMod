package com.capemod.server.services;

/** Erro de negocio/validacao com codigo HTTP; a mensagem e segura para o cliente. */
public final class ApiException extends RuntimeException {
    public final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }
}
