package com.projectx.backend.application.dto;

import com.projectx.backend.domain.models.BankInfo;
import com.projectx.backend.domain.models.ManualPaymentDiscountConfig;
import com.projectx.backend.domain.models.ShippingConfig;
import com.projectx.backend.domain.models.SocialLink;
import com.projectx.backend.domain.models.TenantColors;

import java.util.List;

/**
 * DTO con los campos editables de la configuración de un tenant.
 * Se recibe en el PUT /tenants/:tenantId/config.
 */
public record UpdateTenantRequest(
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
        String thankYouMessage,
        String analyticsId,
        ShippingConfig shippingConfig,
        ManualPaymentDiscountConfig manualPaymentDiscount) {
}
