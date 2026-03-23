package com.projectx.backend.domain.ports.in;

import com.projectx.backend.application.dto.UpdateStatusRequest;
import com.projectx.backend.domain.models.Order;

/**
 * Actualiza el estado de una orden validando transiciones permitidas.
 */
public interface UpdateOrderStatusUseCase {
    Order execute(UpdateStatusRequest request);
}
