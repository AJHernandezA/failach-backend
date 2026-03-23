package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Cart;

public interface UpdateCartItemUseCase {
    Cart execute(String tenantId, String sessionId, String productId, int quantity);
}
