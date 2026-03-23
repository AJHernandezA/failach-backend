package com.projectx.backend.domain.exceptions;

/**
 * Excepción para recursos no encontrados (HTTP 404).
 * Se lanza cuando se busca un recurso que no existe.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
