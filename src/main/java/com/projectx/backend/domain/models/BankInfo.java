package com.projectx.backend.domain.models;

/**
 * Información bancaria del tenant para pagos por transferencia.
 */
public record BankInfo(
        String bankName,
        String accountType,
        String accountNumber,
        String accountHolder,
        String documentType,
        String documentNumber
) {}
