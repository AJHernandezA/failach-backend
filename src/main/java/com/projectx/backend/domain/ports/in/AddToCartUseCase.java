package com.projectx.backend.domain.ports.in;

import com.projectx.backend.application.dto.AddToCartRequest;
import com.projectx.backend.domain.models.Cart;

public interface AddToCartUseCase {
    Cart execute(String tenantId, String sessionId, AddToCartRequest request);
}
