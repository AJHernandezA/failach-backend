package com.projectx.backend.domain.exceptions;

/**
 * Excepción para violaciones de reglas de negocio (HTTP 422).
 * Se lanza cuando una operación viola una regla del dominio.
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super("BUSINESS_RULE_VIOLATION", message);
    }
}
