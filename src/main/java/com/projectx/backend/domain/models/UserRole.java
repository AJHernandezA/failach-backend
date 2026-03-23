package com.projectx.backend.domain.models;

/**
 * Roles del sistema. Define los niveles de acceso.
 * SUPER_ADMIN: acceso total a todos los tenants.
 * MERCHANT: acceso solo a su propio tenant.
 * BUYER: futuro, historial de compras.
 */
public enum UserRole {
    SUPER_ADMIN,
    MERCHANT,
    BUYER
}
