package com.projectx.backend.domain.exceptions;

/**
 * Excepción para conflictos de recursos (HTTP 409).
 * Se lanza cuando se intenta crear un recurso que ya existe.
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}
