package com.projectx.backend.domain.models;

/**
 * Datos del comprador en una orden.
 *
 * @param fullName nombre completo
 * @param email    email de contacto
 * @param phone    teléfono en formato +57XXXXXXXXXX
 */
public record Customer(
        String fullName,
        String email,
        String phone
) {}
