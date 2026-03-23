package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Cart;

public interface RemoveCartItemUseCase {
    Cart execute(String tenantId, String sessionId, String productId);
}
