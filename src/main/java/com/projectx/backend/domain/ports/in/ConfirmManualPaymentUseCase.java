package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Order;

/**
 * Confirma el pago manual de una orden (transferencia bancaria o efectivo).
 */
public interface ConfirmManualPaymentUseCase {
    Order execute(String tenantId, String orderCode, String note);
}
