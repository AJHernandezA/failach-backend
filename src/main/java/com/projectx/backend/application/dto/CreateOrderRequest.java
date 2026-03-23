package com.projectx.backend.application.dto;

/**
 * DTO para crear una orden desde el checkout.
 *
 * @param fullName       nombre completo del comprador
 * @param email          email de contacto
 * @param phone          teléfono (+57XXXXXXXXXX)
 * @param deliveryMethod SHIPPING o PICKUP
 * @param address        dirección (requerida si SHIPPING)
 * @param city           ciudad (requerida si SHIPPING)
 * @param neighborhood   barrio (opcional)
 * @param additionalInfo info adicional (opcional)
 * @param paymentMethod  WOMPI, BANK_TRANSFER o CASH_ON_DELIVERY
 * @param notes          notas del comprador (opcional)
 */
public record CreateOrderRequest(
        String fullName,
        String email,
        String phone,
        String deliveryMethod,
        String address,
        String city,
        String neighborhood,
        String additionalInfo,
        String paymentMethod,
        String notes
) {}
