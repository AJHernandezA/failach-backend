package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Cart;
import com.projectx.backend.domain.models.CartItem;
import com.projectx.backend.domain.ports.in.RemoveCartItemUseCase;
import com.projectx.backend.domain.ports.out.CartRepository;

import java.util.List;

/**
 * Implementación del caso de uso para eliminar un ítem del carrito.
 */
public class RemoveCartItemUseCaseImpl implements RemoveCartItemUseCase {

    private final CartRepository cartRepository;

    @Inject
    public RemoveCartItemUseCaseImpl(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Override
    public Cart execute(String tenantId, String sessionId, String productId) {
        Cart cart = cartRepository.findById(tenantId, sessionId)
                .orElseThrow(() -> new NotFoundException("Carrito no encontrado"));

        List<CartItem> filtered = cart.items().stream()
                .filter(i -> !i.productId().equals(productId))
                .toList();

        if (filtered.size() == cart.items().size()) {
            throw new NotFoundException("Producto no encontrado en el carrito");
        }

        Cart updated = cart.withItems(filtered);
        cartRepository.save(updated);
        return updated;
    }
}
