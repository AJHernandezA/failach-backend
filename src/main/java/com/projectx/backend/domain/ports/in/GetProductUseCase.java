package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Product;

public interface GetProductUseCase {
    Product execute(String tenantId, String productId);
}
