package com.projectx.backend.domain.models;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configuración de descuento por pago manual (transferencia/efectivo) por tenant.
 */
public record ManualPaymentDiscountConfig(
        boolean enabled,
        BigDecimal discountRate,
        String discountLabel,
        List<String> applicableMethods
) {}
