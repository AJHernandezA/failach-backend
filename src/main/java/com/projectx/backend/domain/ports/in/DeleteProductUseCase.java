package com.projectx.backend.domain.ports.in;

public interface DeleteProductUseCase {
    void execute(String tenantId, String productId);
}
