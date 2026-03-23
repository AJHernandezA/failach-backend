package com.projectx.backend.domain.exceptions;

/**
 * Excepción para errores de validación de input (HTTP 400).
 * Se lanza cuando los datos enviados por el cliente son inválidos.
 */
public class BadRequestException extends DomainException {

    public BadRequestException(String message) {
        super("BAD_REQUEST", message);
    }
}
