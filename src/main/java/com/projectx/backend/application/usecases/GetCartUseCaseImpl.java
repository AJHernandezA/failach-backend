package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.Cart;
import com.projectx.backend.domain.ports.in.GetCartUseCase;
import com.projectx.backend.domain.ports.out.CartRepository;

/**
 * Implementación del caso de uso para obtener el carrito.
 * Si no existe, retorna un carrito vacío.
 */
public class GetCartUseCaseImpl implements GetCartUseCase {

    private final CartRepository cartRepository;

    @Inject
    public GetCartUseCaseImpl(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Override
    public Cart execute(String tenantId, String sessionId) {
        return cartRepository.findById(tenantId, sessionId)
                .orElse(Cart.empty(sessionId, tenantId));
    }
}
