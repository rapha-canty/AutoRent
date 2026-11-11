package com.autorent.config;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException naoEncontrado(String recurso) {
        return new ApiException(HttpStatus.NOT_FOUND, recurso + " não encontrado(a).");
    }

    public static ApiException regra(String mensagem) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, mensagem);
    }

    public static ApiException conflito(String mensagem) {
        return new ApiException(HttpStatus.CONFLICT, mensagem);
    }

    public static ApiException proibido(String mensagem) {
        return new ApiException(HttpStatus.FORBIDDEN, mensagem);
    }
}
