package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Order;

/**
 * Cancela una orden y restaura stock si fue descontado.
 */
public interface CancelOrderUseCase {
    Order execute(String tenantId, String orderCode, String reason);
}
