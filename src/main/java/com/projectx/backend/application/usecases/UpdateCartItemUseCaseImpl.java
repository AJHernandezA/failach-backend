package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Cart;
import com.projectx.backend.domain.models.CartItem;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.ports.in.UpdateCartItemUseCase;
import com.projectx.backend.domain.ports.out.CartRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del caso de uso para actualizar la cantidad de un ítem en el carrito.
 * Valida stock disponible.
 */
public class UpdateCartItemUseCaseImpl implements UpdateCartItemUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Inject
    public UpdateCartItemUseCaseImpl(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Cart execute(String tenantId, String sessionId, String productId, int quantity) {
        if (quantity < 1) {
            throw new BadRequestException("La cantidad mínima es 1");
        }

        Cart cart = cartRepository.findById(tenantId, sessionId)
                .orElseThrow(() -> new NotFoundException("Carrito no encontrado"));

        // Buscar ítem en el carrito
        List<CartItem> items = new ArrayList<>(cart.items());
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(productId)) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            throw new NotFoundException("Producto no encontrado en el carrito");
        }

        // Validar stock
        Product product = productRepository.findById(tenantId, productId).orElse(null);
        if (product != null && quantity > product.stock()) {
            throw new BusinessRuleException("Stock insuficiente. Disponible: " + product.stock());
        }

        items.set(index, items.get(index).withQuantity(quantity));
        Cart updated = cart.withItems(items);
        cartRepository.save(updated);
        return updated;
    }
}
