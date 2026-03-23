package com.projectx.backend.domain.ports.in;

public interface ClearCartUseCase {
    void execute(String tenantId, String sessionId);
}
