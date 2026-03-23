package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Cart;

public interface GetCartUseCase {
    Cart execute(String tenantId, String sessionId);
}
