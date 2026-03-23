package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.Cart;

import java.util.Optional;

/**
 * Puerto de salida para persistencia de carritos de compras.
 */
public interface CartRepository {

    Optional<Cart> findById(String tenantId, String sessionId);

    void save(Cart cart);

    void delete(String tenantId, String sessionId);
}
