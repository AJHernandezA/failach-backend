package com.projectx.backend.domain.ports.in;

import com.projectx.backend.application.dto.CreateProductRequest;
import com.projectx.backend.domain.models.Product;

public interface CreateProductUseCase {
    Product execute(String tenantId, CreateProductRequest request);
}
