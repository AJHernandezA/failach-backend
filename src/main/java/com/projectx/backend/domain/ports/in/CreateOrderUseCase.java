package com.projectx.backend.domain.ports.in;

import com.projectx.backend.application.dto.CreateOrderRequest;
import com.projectx.backend.domain.models.Order;

public interface CreateOrderUseCase {
    Order execute(String tenantId, String sessionId, CreateOrderRequest request);
}
