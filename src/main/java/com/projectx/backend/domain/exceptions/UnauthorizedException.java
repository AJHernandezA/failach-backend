package com.projectx.backend.domain.exceptions;

/**
 * Excepción para errores de autenticación (HTTP 401).
 * Se lanza cuando el usuario no está autenticado o el token es inválido.
 */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
