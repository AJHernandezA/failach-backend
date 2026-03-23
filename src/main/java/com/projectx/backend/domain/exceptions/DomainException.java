package com.projectx.backend.domain.exceptions;

/**
 * Excepción base del dominio.
 * Todas las excepciones de negocio extienden de esta clase.
 * Contiene un código de error que se usa en las respuestas HTTP.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
