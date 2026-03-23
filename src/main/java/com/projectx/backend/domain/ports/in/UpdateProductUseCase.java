package com.projectx.backend.domain.ports.in;

import com.projectx.backend.application.dto.UpdateProductRequest;
import com.projectx.backend.domain.models.Product;

public interface UpdateProductUseCase {
    Product execute(String tenantId, String productId, UpdateProductRequest request);
}
