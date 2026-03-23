package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.ports.in.ClearCartUseCase;
import com.projectx.backend.domain.ports.out.CartRepository;

/**
 * Implementación del caso de uso para vaciar el carrito.
 */
public class ClearCartUseCaseImpl implements ClearCartUseCase {

    private final CartRepository cartRepository;

    @Inject
    public ClearCartUseCaseImpl(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Override
    public void execute(String tenantId, String sessionId) {
        cartRepository.delete(tenantId, sessionId);
    }
}
