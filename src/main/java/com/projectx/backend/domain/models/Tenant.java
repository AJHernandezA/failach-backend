package com.projectx.backend.domain.models;

import java.time.Instant;
import java.util.List;

/**
 * Entidad principal del dominio que representa un tenant (tienda).
 * Contiene toda la configuración visual, de contacto y operacional.
 */
public record Tenant(
        String tenantId,
        String name,
        String description,
        String logoUrl,
        String faviconUrl,
        String bannerUrl,
        TenantColors colors,
        String font,
        List<SocialLink> socialMedia,
        List<String> cities,
        String whatsapp,
        String email,
        String phone,
        String address,
        String schedule,
        BankInfo bankInfo,
        boolean isActive,
        String thankYouMessage,
        String analyticsId,
        ShippingConfig shippingConfig,
        ManualPaymentDiscountConfig manualPaymentDiscount,
        Instant createdAt,
        Instant updatedAt) {
}
