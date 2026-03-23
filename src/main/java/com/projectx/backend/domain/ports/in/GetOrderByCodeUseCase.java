package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Order;

public interface GetOrderByCodeUseCase {
    Order execute(String tenantId, String orderCode);
}
