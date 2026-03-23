package com.projectx.backend.domain.exceptions;

/**
 * Excepción para errores de autorización (HTTP 403).
 * Se lanza cuando el usuario no tiene permisos para la operación.
 */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
