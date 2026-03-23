package com.projectx.backend.domain.models;

import java.math.BigDecimal;

/**
 * Configuración de envío por tenant.
 */
public record ShippingConfig(
        BigDecimal defaultShippingCost,
        BigDecimal freeShippingThreshold,
        boolean pickupEnabled,
        boolean pickupDiscountEnabled,
        String shippingLabel
) {}
