package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.AddToCartRequest;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.AddToCartUseCase;
import com.projectx.backend.domain.ports.out.CartRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del caso de uso para agregar un producto al carrito.
 * Valida existencia del producto, stock disponible, y maneja duplicados sumando cantidad.
 */
public class AddToCartUseCaseImpl implements AddToCartUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Inject
    public AddToCartUseCaseImpl(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Cart execute(String tenantId, String sessionId, AddToCartRequest request) {
        // Validar cantidad
        if (request.quantity() < 1) {
            throw new BadRequestException("La cantidad mínima es 1");
        }

        // Verificar que el producto existe y está activo
        Product product = productRepository.findById(tenantId, request.productId())
                .orElseThrow(() -> new NotFoundException("Producto no encontrado o no disponible"));

        if (product.status() != ProductStatus.ACTIVE) {
            throw new NotFoundException("Producto no encontrado o no disponible");
        }

        // Obtener o crear carrito
        Cart cart = cartRepository.findById(tenantId, sessionId)
                .orElse(Cart.empty(sessionId, tenantId));

        // Verificar si ya existe el producto en el carrito
        List<CartItem> items = new ArrayList<>(cart.items());
        int existingIndex = -1;
        int existingQuantity = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(request.productId())) {
                existingIndex = i;
                existingQuantity = items.get(i).quantity();
                break;
            }
        }

        int totalQuantity = existingQuantity + request.quantity();

        // Validar stock disponible
        if (totalQuantity > product.stock()) {
            throw new BusinessRuleException("Stock insuficiente. Disponible: " + product.stock());
        }

        // Resolver imagen
        String imageUrl = product.images().isEmpty() ? "" : product.images().get(0);

        // Resolver variante
        String variantId = request.variantId();
        String variantName = null;
        if (variantId != null && !variantId.isBlank()) {
            variantName = product.variants().stream()
                    .filter(v -> v.variantId().equals(variantId))
                    .map(ProductVariant::name)
                    .findFirst()
                    .orElse(null);
        }

        if (existingIndex >= 0) {
            // Actualizar cantidad del ítem existente
            items.set(existingIndex, items.get(existingIndex).withQuantity(totalQuantity));
        } else {
            // Agregar nuevo ítem
            items.add(new CartItem(
                    product.productId(),
                    product.name(),
                    product.price(),
                    request.quantity(),
                    imageUrl,
                    variantId,
                    variantName
            ));
        }

        Cart updated = cart.withItems(items);
        cartRepository.save(updated);
        return updated;
    }
}
